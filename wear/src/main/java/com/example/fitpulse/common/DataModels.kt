package com.example.fitpulse.common

import org.json.JSONObject

/**
 * Common data models shared by the Phone and the Watch.
 *
 * The Data Layer transports raw [ByteArray]s, so every model implements a
 * small JSON serializer/deserializer with org.json (bundled with Android).
 * Keeping the exact same model in both modules guarantees both sides speak
 * the same wire format.
 */

/** Command sent by the watch when the user starts or stops a workout. */
data class WorkoutCommand(
    val type: String,
    val timestamp: Long,
    val elapsedSeconds: Long = 0L,
) {
    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put(KEY_TYPE, type)
        put(KEY_TIMESTAMP, timestamp)
        put(KEY_ELAPSED, elapsedSeconds)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_ELAPSED = "elapsedSeconds"

        fun fromJsonBytes(bytes: ByteArray?): WorkoutCommand? = runCatching {
            val json = JSONObject(String(bytes ?: return null, Charsets.UTF_8))
            WorkoutCommand(
                type = json.getString(KEY_TYPE),
                timestamp = json.getLong(KEY_TIMESTAMP),
                elapsedSeconds = json.optLong(KEY_ELAPSED, 0L),
            )
        }.getOrNull()
    }
}

/** Acknowledgement sent back by the phone after processing a [WorkoutCommand]. */
data class WorkoutAck(
    val status: String,
    val message: String,
    val sessionId: Long = 0L,
    val durationSeconds: Long = 0L,
    val calories: Int = 0,
    val dailyGoal: Int = WearableConstants.DEFAULT_DAILY_GOAL,
) {
    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put(KEY_STATUS, status)
        put(KEY_MESSAGE, message)
        put(KEY_SESSION_ID, sessionId)
        put(KEY_DURATION, durationSeconds)
        put(KEY_CALORIES, calories)
        put(KEY_DAILY_GOAL, dailyGoal)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        private const val KEY_STATUS = "status"
        private const val KEY_MESSAGE = "message"
        private const val KEY_SESSION_ID = "sessionId"
        private const val KEY_DURATION = "durationSeconds"
        private const val KEY_CALORIES = "calories"
        private const val KEY_DAILY_GOAL = "dailyGoal"

        fun fromJsonBytes(bytes: ByteArray?): WorkoutAck? = runCatching {
            val json = JSONObject(String(bytes ?: return null, Charsets.UTF_8))
            WorkoutAck(
                status = json.getString(KEY_STATUS),
                message = json.optString(KEY_MESSAGE, ""),
                sessionId = json.optLong(KEY_SESSION_ID, 0L),
                durationSeconds = json.optLong(KEY_DURATION, 0L),
                calories = json.optInt(KEY_CALORIES, 0),
                dailyGoal = json.optInt(KEY_DAILY_GOAL, WearableConstants.DEFAULT_DAILY_GOAL),
            )
        }.getOrNull()
    }
}

/** A finished workout, stored on the phone (the authoritative hub). */
data class WorkoutSession(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val calories: Int,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_STARTED_AT, startedAt)
        put(KEY_ENDED_AT, endedAt)
        put(KEY_DURATION, durationSeconds)
        put(KEY_CALORIES, calories)
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_STARTED_AT = "startedAt"
        private const val KEY_ENDED_AT = "endedAt"
        private const val KEY_DURATION = "durationSeconds"
        private const val KEY_CALORIES = "calories"

        fun fromJson(json: JSONObject): WorkoutSession = WorkoutSession(
            id = json.getLong(KEY_ID),
            startedAt = json.getLong(KEY_STARTED_AT),
            endedAt = json.getLong(KEY_ENDED_AT),
            durationSeconds = json.getLong(KEY_DURATION),
            calories = json.optInt(KEY_CALORIES, 0),
        )
    }
}
