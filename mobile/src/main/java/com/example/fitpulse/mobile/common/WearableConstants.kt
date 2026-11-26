package com.example.fitpulse.mobile.common

/**
 * Single source of truth for every path / key used on the Wearable Data Layer.
 *
 * IMPORTANT: this file is duplicated in the mobile module with the same values.
 * Both APKs must agree on the exact strings or the messages/data items will
 * never reach the other device.
 */
object WearableConstants {

    // ---------------------------------------------------------------------
    // Feature 1 - MessageClient (commands and events)
    // ---------------------------------------------------------------------

    /** Watch -> Phone: the user pressed START. Payload = [WorkoutCommand] JSON. */
    const val PATH_START_WORKOUT = "/fitpulse/workout/start"

    /** Watch -> Phone: the user pressed STOP. Payload = [WorkoutCommand] JSON. */
    const val PATH_STOP_WORKOUT = "/fitpulse/workout/stop"

    /** Phone -> Watch: acknowledgement of a START/STOP command. Payload = [WorkoutAck] JSON. */
    const val PATH_WORKOUT_ACK = "/fitpulse/workout/ack"

    const val CMD_START = "START"
    const val CMD_STOP = "STOP"

    const val ACK_STARTED = "STARTED"
    const val ACK_STOPPED = "STOPPED"
    const val ACK_ERROR = "ERROR"

    // ---------------------------------------------------------------------
    // Feature 2 - DataClient (configuration synchronization)
    // ---------------------------------------------------------------------

    /**
     * Phone -> Watch (replicated data item): the daily step goal.
     * The phone is the authoritative writer; the watch only reads it.
     */
    const val PATH_DAILY_GOAL = "/fitpulse/config/daily_goal"

    /** Key inside the DataMap that stores the goal as an Int. */
    const val KEY_DAILY_GOAL = "daily_step_goal"

    /** Key inside the DataMap that stores when the goal was last changed (epoch millis). */
    const val KEY_GOAL_UPDATED_AT = "goal_updated_at"

    const val DEFAULT_DAILY_GOAL = 8_000
    const val MIN_DAILY_GOAL = 2_000
    const val MAX_DAILY_GOAL = 20_000
    const val GOAL_STEP = 500

    /** Calories burned per minute of an active workout (demo approximation). */
    const val CALORIES_PER_MINUTE = 8.0
}
