package com.example.fitpulse.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.fitpulse.mobile.common.WearableConstants
import com.example.fitpulse.mobile.common.WorkoutAck
import com.example.fitpulse.mobile.common.WorkoutCommand
import com.example.fitpulse.mobile.common.WorkoutSession
import com.example.fitpulse.mobile.data.FitPulseRepository
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * FitPulse - Phone hub (authoritative device).
 *
 * Responsibilities:
 *  - Feature 1 (MessageClient): receive START/STOP commands from the watch,
 *    run the authoritative session calculation, persist the session and reply
 *    with an acknowledgement carrying the computed summary.
 *  - Feature 2 (DataClient): own the daily step goal, persist it and push it
 *    to the watch as a replicated data item.
 */
class MainActivity : ComponentActivity(),
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val tag = "FitPulsePhone"

    private lateinit var messageClient: MessageClient
    private lateinit var dataClient: DataClient
    private lateinit var repository: FitPulseRepository

    // ------------------------------ UI state ------------------------------
    private var isWorkoutActive by mutableStateOf(false)
    private var workoutStartMillis by mutableLongStateOf(0L)
    private var clockMillis by mutableLongStateOf(System.currentTimeMillis())
    private var statusMessage by mutableStateOf("Waiting for the watch to start a workout…")
    private var dailyGoal by mutableIntStateOf(WearableConstants.DEFAULT_DAILY_GOAL)
    private var goalDraft by mutableFloatStateOf(WearableConstants.DEFAULT_DAILY_GOAL.toFloat())
    private var sessions by mutableStateOf<List<WorkoutSession>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = FitPulseRepository(this)
        messageClient = Wearable.getMessageClient(this)
        dataClient = Wearable.getDataClient(this)

        dailyGoal = repository.getDailyGoal()
        goalDraft = dailyGoal.toFloat()
        sessions = repository.getSessions()

        setContent {
            // Ticks once per second while a workout is active so the elapsed
            // time on the phone stays in sync with the watch.
            LaunchedEffect(isWorkoutActive) {
                while (isWorkoutActive) {
                    clockMillis = System.currentTimeMillis()
                    delay(1_000)
                }
            }

            FitPulsePhoneTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PhoneHubScreen(
                        isWorkoutActive = isWorkoutActive,
                        elapsedSeconds = elapsedSeconds(),
                        statusMessage = statusMessage,
                        dailyGoal = dailyGoal,
                        goalDraft = goalDraft,
                        sessions = sessions,
                        onGoalDraftChange = { goalDraft = it },
                        onSaveGoal = ::publishDailyGoal,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Listeners stay registered while the UI is visible. In a production
        // app they would live in a foreground service so the phone can answer
        // commands even when the screen is off.
        messageClient.addListener(this)
        dataClient.addListener(this)
    }

    override fun onPause() {
        messageClient.removeListener(this)
        dataClient.removeListener(this)
        super.onPause()
    }

    /** Drives the on-screen timer while a workout is running. */
    private fun elapsedSeconds(): Long {
        if (!isWorkoutActive) return 0L
        return ((clockMillis - workoutStartMillis) / 1000L).coerceAtLeast(0L)
    }

    // ---------------------------------------------------------------------
    // Feature 1 - MessageClient: the watch sends commands, the phone replies.
    // ---------------------------------------------------------------------

    /**
     * One listener handles both features 1 paths. The `when` on `messageEvent.path`
     * is what prevents START and STOP commands from colliding.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val command = WorkoutCommand.fromJsonBytes(messageEvent.data)
        Log.d(tag, "Message received on ${messageEvent.path}: $command")

        when (messageEvent.path) {
            WearableConstants.PATH_START_WORKOUT -> {
                command ?: return sendAck(
                    messageEvent.sourceNodeId,
                    WorkoutAck(WearableConstants.ACK_ERROR, "Malformed START payload"),
                )
                handleStartCommand(messageEvent.sourceNodeId)
            }

            WearableConstants.PATH_STOP_WORKOUT -> {
                command ?: return sendAck(
                    messageEvent.sourceNodeId,
                    WorkoutAck(WearableConstants.ACK_ERROR, "Malformed STOP payload"),
                )
                handleStopCommand(messageEvent.sourceNodeId)
            }
        }
    }

    /** Authoritative start: the phone decides the official start time. */
    private fun handleStartCommand(nodeId: String) {
        if (isWorkoutActive) {
            sendAck(
                nodeId,
                WorkoutAck(WearableConstants.ACK_ERROR, "A workout is already in progress"),
            )
            return
        }

        val now = System.currentTimeMillis()
        workoutStartMillis = now
        clockMillis = now
        isWorkoutActive = true
        statusMessage = "Watch started a workout. Logging live…"

        sendAck(
            nodeId,
            WorkoutAck(
                status = WearableConstants.ACK_STARTED,
                message = "Workout started",
                sessionId = now,
                dailyGoal = dailyGoal,
            ),
        )
    }

    /** Authoritative stop: duration and calories are computed from the phone clock. */
    private fun handleStopCommand(nodeId: String) {
        if (!isWorkoutActive) {
            sendAck(
                nodeId,
                WorkoutAck(WearableConstants.ACK_ERROR, "There is no active workout to stop"),
            )
            return
        }

        val now = System.currentTimeMillis()
        val durationSeconds = ((now - workoutStartMillis) / 1000L).coerceAtLeast(1L)
        val calories = ceil(durationSeconds / 60.0 * WearableConstants.CALORIES_PER_MINUTE).toInt()

        val session = WorkoutSession(
            id = workoutStartMillis,
            startedAt = workoutStartMillis,
            endedAt = now,
            durationSeconds = durationSeconds,
            calories = calories,
        )
        sessions = repository.addSession(session)

        isWorkoutActive = false
        clockMillis = now
        statusMessage = "Session saved · ${formatDuration(durationSeconds)} · $calories kcal"

        sendAck(
            nodeId,
            WorkoutAck(
                status = WearableConstants.ACK_STOPPED,
                message = "Workout saved",
                sessionId = session.id,
                durationSeconds = durationSeconds,
                calories = calories,
                dailyGoal = dailyGoal,
            ),
        )
    }

    private fun sendAck(nodeId: String, ack: WorkoutAck) {
        lifecycleScope.launch {
            runCatching {
                messageClient
                    .sendMessage(nodeId, WearableConstants.PATH_WORKOUT_ACK, ack.toJsonBytes())
                    .await()
            }.onSuccess {
                Log.d(tag, "ACK ${ack.status} sent to $nodeId")
            }.onFailure { error ->
                Log.e(tag, "Could not send ACK: ${error.message}")
                statusMessage = "Could not reach the watch: ${error.message}"
            }
        }
    }

    // ---------------------------------------------------------------------
    // Feature 2 - DataClient: the phone is the only writer of the goal.
    // ---------------------------------------------------------------------

    /**
     * Handles replicated data-item changes. The path check distinguishes the
     * goal feature from any future DataClient feature.
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            dataEvents.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED) return@forEach
                val item = event.dataItem ?: return@forEach
                if (item.uri.path != WearableConstants.PATH_DAILY_GOAL) return@forEach

                val map = DataMap.fromByteArray(item.data ?: return@forEach)
                val steps = map.getInt(WearableConstants.KEY_DAILY_GOAL, dailyGoal)
                val updatedAt = map.getLong(WearableConstants.KEY_GOAL_UPDATED_AT, 0L)

                // Our own put also comes back here; only adopt different values.
                if (steps != dailyGoal) {
                    dailyGoal = steps
                    goalDraft = steps.toFloat()
                    repository.saveDailyGoal(steps, updatedAt)
                }
            }
        } finally {
            dataEvents.release()
        }
    }

    /** Persists the goal locally and replicates it to the watch. */
    private fun publishDailyGoal() {
        val steps = (goalDraft / WearableConstants.GOAL_STEP)
            .roundToInt()
            .times(WearableConstants.GOAL_STEP)
            .coerceIn(WearableConstants.MIN_DAILY_GOAL, WearableConstants.MAX_DAILY_GOAL)
        val updatedAt = System.currentTimeMillis()

        dailyGoal = steps
        goalDraft = steps.toFloat()
        repository.saveDailyGoal(steps, updatedAt)
        statusMessage = "Goal set to ${formatSteps(steps)} steps. Syncing…"

        lifecycleScope.launch {
            runCatching {
                val map = DataMap().apply {
                    putInt(WearableConstants.KEY_DAILY_GOAL, steps)
                    putLong(WearableConstants.KEY_GOAL_UPDATED_AT, updatedAt)
                }
                // create() builds wear://<node>/fitpulse/config/daily_goal for us.
                val request = PutDataRequest.create(WearableConstants.PATH_DAILY_GOAL).apply {
                    data = map.toByteArray()
                }
                dataClient.putDataItem(request).await()
            }.onSuccess {
                statusMessage = "Goal ${formatSteps(steps)} steps synced to the watch"
            }.onFailure { error ->
                Log.e(tag, "Goal sync failed: ${error.message}")
                statusMessage = "Goal saved on the phone; watch unreachable"
            }
        }
    }
}

// -------------------------------------------------------------------------
// Compose UI - phone prototype
// -------------------------------------------------------------------------

private val FitPulseGreen = Color(0xFF00875A)

@Composable
private fun FitPulsePhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = FitPulseGreen, secondary = Color(0xFF00A36C)),
        content = content,
    )
}

@Composable
private fun PhoneHubScreen(
    isWorkoutActive: Boolean,
    elapsedSeconds: Long,
    statusMessage: String,
    dailyGoal: Int,
    goalDraft: Float,
    sessions: List<WorkoutSession>,
    onGoalDraftChange: (Float) -> Unit,
    onSaveGoal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FitPulse", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("PHONE HUB", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("Kevin Noh\nKenny Peralta", fontSize = 11.sp, lineHeight = 14.sp)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "LIVE WORKOUT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (isWorkoutActive) "● ACTIVE" else "○ IDLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWorkoutActive) FitPulseGreen else MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = formatDuration(elapsedSeconds),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("elapsed", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(10.dp))
                Text(statusMessage, fontSize = 14.sp)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "DAILY STEP GOAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatSteps(dailyGoal),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("steps", fontSize = 14.sp, modifier = Modifier.padding(bottom = 5.dp))
                }
                Slider(
                    value = goalDraft,
                    onValueChange = onGoalDraftChange,
                    valueRange = WearableConstants.MIN_DAILY_GOAL.toFloat()..WearableConstants.MAX_DAILY_GOAL.toFloat(),
                    steps = (WearableConstants.MAX_DAILY_GOAL - WearableConstants.MIN_DAILY_GOAL) /
                        WearableConstants.GOAL_STEP - 1,
                )
                Text(
                    "Selected: ${formatSteps(goalDraft.roundToInt())} steps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onSaveGoal, modifier = Modifier.fillMaxWidth()) {
                    Text("Sync goal to watch")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "WORKOUT HISTORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                if (sessions.isEmpty()) {
                    Text(
                        "No sessions yet. Start one from the watch.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    sessions.take(5).forEachIndexed { index, session ->
                        if (index > 0) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                        SessionRow(session)
                    }
                }
            }
        }

        Text(
            "Kevin Noh & Kenny Peralta · FitPulse",
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SessionRow(session: WorkoutSession) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d · HH:mm", Locale.getDefault()) }
    val whenText = remember(session.id) {
        Instant.ofEpochMilli(session.startedAt)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(whenText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Started from watch",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Text(
            "${formatDuration(session.durationSeconds)} · ${session.calories} kcal",
            fontSize = 13.sp,
        )
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
