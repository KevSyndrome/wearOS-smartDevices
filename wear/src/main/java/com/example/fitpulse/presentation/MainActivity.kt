package com.example.fitpulse.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.fitpulse.common.WearableConstants
import com.example.fitpulse.common.WorkoutAck
import com.example.fitpulse.common.WorkoutCommand
import com.example.fitpulse.presentation.theme.FitPulseTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * FitPulse - Wear OS client.
 *
 * Responsibilities:
 *  - Feature 1 (MessageClient): send START/STOP commands to the phone and
 *    react to the phone's acknowledgement.
 *  - Feature 2 (DataClient): listen for the phone's daily step goal and show
 *    it at a glance.
 */
class MainActivity : ComponentActivity(),
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val tag = "FitPulseWatch"

    private lateinit var messageClient: MessageClient
    private lateinit var dataClient: DataClient

    // ------------------------------ UI state ------------------------------
    private var isWorkoutActive by mutableStateOf(false)
    private var isSending by mutableStateOf(false)
    private var workoutStartMillis by mutableLongStateOf(0L)
    private var clockMillis by mutableLongStateOf(System.currentTimeMillis())
    private var stoppedDuration by mutableLongStateOf(0L)
    private var statusMessage by mutableStateOf("Tap to start a workout")
    private var dailyGoal by mutableIntStateOf(WearableConstants.DEFAULT_DAILY_GOAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageClient = Wearable.getMessageClient(this)
        dataClient = Wearable.getDataClient(this)

        setContent {
            FitPulseTheme {
                LaunchedEffect(isWorkoutActive) {
                    while (isWorkoutActive) {
                        clockMillis = System.currentTimeMillis()
                        delay(1_000)
                    }
                }

                val elapsed = if (isWorkoutActive) {
                    ((clockMillis - workoutStartMillis) / 1_000L).coerceAtLeast(0L)
                } else {
                    stoppedDuration
                }

                WatchScreen(
                    isWorkoutActive = isWorkoutActive,
                    isSending = isSending,
                    elapsedSeconds = elapsed,
                    dailyGoal = dailyGoal,
                    statusMessage = statusMessage,
                    onToggleWorkout = {
                        if (isWorkoutActive) sendCommand(WearableConstants.CMD_STOP)
                        else sendCommand(WearableConstants.CMD_START)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(this)
        dataClient.addListener(this)
        readCurrentGoal()
    }

    override fun onPause() {
        messageClient.removeListener(this)
        dataClient.removeListener(this)
        super.onPause()
    }

    // ---------------------------------------------------------------------
    // Feature 2 - DataClient: read the replicated goal the phone owns.
    // ---------------------------------------------------------------------

    /** Reads the already-replicated data item, e.g. when the watch app opens. */
    private fun readCurrentGoal() {
        lifecycleScope.launch {
            runCatching {
                val items = dataClient.getDataItems().await()
                try {
                    val goalItem = items.firstOrNull {
                        it.uri.path == WearableConstants.PATH_DAILY_GOAL
                    } ?: return@runCatching null
                    DataMap.fromByteArray(goalItem.data ?: return@runCatching null)
                        .getInt(WearableConstants.KEY_DAILY_GOAL, dailyGoal)
                } finally {
                    items.release()
                }
            }.onSuccess { steps ->
                if (steps != null) dailyGoal = steps
            }.onFailure { error ->
                Log.d(tag, "No goal available yet: ${error.message}")
            }
        }
    }

    /** Called whenever the phone writes/updates the goal data item. */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            dataEvents.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED) return@forEach
                val item = event.dataItem ?: return@forEach
                if (item.uri.path != WearableConstants.PATH_DAILY_GOAL) return@forEach

                val steps = DataMap.fromByteArray(item.data ?: return@forEach)
                    .getInt(WearableConstants.KEY_DAILY_GOAL, dailyGoal)
                Log.d(tag, "Goal synced from phone: $steps")
                dailyGoal = steps
            }
        } finally {
            dataEvents.release()
        }
    }

    // ---------------------------------------------------------------------
    // Feature 1 - MessageClient: send commands, consume acknowledgements.
    // ---------------------------------------------------------------------

    private fun sendCommand(type: String) {
        if (isSending) return
        if (type == WearableConstants.CMD_START && isWorkoutActive) return
        if (type == WearableConstants.CMD_STOP && !isWorkoutActive) return

        isSending = true
        statusMessage = when (type) {
            WearableConstants.CMD_START -> "Sending START…"
            else -> "Sending STOP…"
        }

        lifecycleScope.launch {
            runCatching {
                // 1. Discover the paired phone(s).
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                check(nodes.isNotEmpty()) { "No phone connected" }

                // 2. Build the typed payload once and send it to every node.
                val elapsed = if (isWorkoutActive) {
                    ((System.currentTimeMillis() - workoutStartMillis) / 1_000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                val command = WorkoutCommand(
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    elapsedSeconds = elapsed,
                )
                val path = when (type) {
                    WearableConstants.CMD_START -> WearableConstants.PATH_START_WORKOUT
                    else -> WearableConstants.PATH_STOP_WORKOUT
                }

                nodes.forEach { node ->
                    Log.d(tag, "Sending $type to ${node.displayName} (${node.id})")
                    messageClient.sendMessage(node.id, path, command.toJsonBytes()).await()
                }
            }.onSuccess {
                statusMessage = when (type) {
                    WearableConstants.CMD_START -> "START sent. Waiting…"
                    else -> "STOP sent. Waiting…"
                }
            }.onFailure { error ->
                Log.e(tag, "Command failed: ${error.message}")
                statusMessage = error.message ?: "Could not reach the phone"
            }
            isSending = false
        }
    }

    /**
     * The phone replies on PATH_WORKOUT_ACK. The status inside the JSON tells
     * the watch whether the workout actually started, stopped or failed.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearableConstants.PATH_WORKOUT_ACK) return

        val ack = WorkoutAck.fromJsonBytes(messageEvent.data)
        if (ack == null) {
            statusMessage = "Malformed acknowledgement from phone"
            return
        }
        Log.d(tag, "ACK received: $ack")

        isSending = false
        when (ack.status) {
            WearableConstants.ACK_STARTED -> {
                workoutStartMillis = System.currentTimeMillis()
                clockMillis = workoutStartMillis
                stoppedDuration = 0L
                isWorkoutActive = true
                statusMessage = "Workout active"
            }

            WearableConstants.ACK_STOPPED -> {
                isWorkoutActive = false
                stoppedDuration = ack.durationSeconds
                statusMessage = "Saved ${formatDuration(ack.durationSeconds)} · ${ack.calories} kcal"
            }

            else -> {
                isWorkoutActive = false
                statusMessage = ack.message.ifBlank { "Phone rejected the command" }
            }
        }
        if (ack.dailyGoal > 0) dailyGoal = ack.dailyGoal
    }
}

// -------------------------------------------------------------------------
// Compose UI - Wear OS prototype (glanceable, one-tap control)
// -------------------------------------------------------------------------

@Composable
private fun WatchScreen(
    isWorkoutActive: Boolean,
    isSending: Boolean,
    elapsedSeconds: Long,
    dailyGoal: Int,
    statusMessage: String,
    onToggleWorkout: () -> Unit,
) {
    AppScaffold {
        ScreenScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Keep clear of the system time that AppScaffold draws at the top.
                Spacer(Modifier.height(14.dp))
                Text(
                    "FITPULSE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Goal ${formatSteps(dailyGoal)} steps",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDuration(elapsedSeconds),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWorkoutActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = if (isWorkoutActive) "WORKOUT ACTIVE" else "READY",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onToggleWorkout,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth(0.8f),
                ) {
                    Text(if (isWorkoutActive) "STOP WORKOUT" else "START WORKOUT")
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusMessage,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Kevin Noh · Kenny Peralta",
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------ helpers ------------------------------

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun formatSteps(steps: Int): String = String.format(Locale.US, "%,d", steps)

@WearPreviewDevices
@Composable
private fun WatchPreview() {
    FitPulseTheme {
        WatchScreen(
            isWorkoutActive = true,
            isSending = false,
            elapsedSeconds = 754,
            dailyGoal = 8_000,
            statusMessage = "Workout active",
            onToggleWorkout = {},
        )
    }
}
