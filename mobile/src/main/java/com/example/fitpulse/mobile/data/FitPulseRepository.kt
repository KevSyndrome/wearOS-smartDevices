package com.example.fitpulse.mobile.data

import android.content.Context
import com.example.fitpulse.mobile.common.WearableConstants
import com.example.fitpulse.mobile.common.WorkoutSession
import org.json.JSONArray

/**
 * Authoritative local storage for the phone hub.
 *
 * The phone owns the configuration (daily step goal) and the workout history.
 * The watch never stores this data; it only sends commands and mirrors the
 * configuration it receives through the Data Layer.
 */
class FitPulseRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ----------------------------- Feature 2: goal -----------------------------

    fun getDailyGoal(): Int = prefs.getInt(KEY_DAILY_GOAL, WearableConstants.DEFAULT_DAILY_GOAL)

    fun getGoalUpdatedAt(): Long = prefs.getLong(KEY_GOAL_UPDATED_AT, 0L)

    fun saveDailyGoal(steps: Int, updatedAt: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putInt(KEY_DAILY_GOAL, steps)
            .putLong(KEY_GOAL_UPDATED_AT, updatedAt)
            .apply()
    }

    // --------------------------- Feature 1: workouts ---------------------------

    /** Newest first, capped so SharedPreferences stays small. */
    fun getSessions(): List<WorkoutSession> = runCatching {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                add(WorkoutSession.fromJson(array.getJSONObject(index)))
            }
        }
    }.getOrDefault(emptyList())

    fun addSession(session: WorkoutSession): List<WorkoutSession> {
        val updated = (listOf(session) + getSessions()).take(MAX_SESSIONS)
        val array = JSONArray()
        updated.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
        return updated
    }

    private companion object {
        const val PREFS_NAME = "fitpulse_hub_prefs"
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_GOAL_UPDATED_AT = "goal_updated_at"
        const val KEY_SESSIONS = "workout_sessions"
        const val MAX_SESSIONS = 20
    }
}
