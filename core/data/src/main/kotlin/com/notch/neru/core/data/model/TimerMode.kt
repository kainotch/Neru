package com.notch.neru.core.data.model

/**
 * Operating mode for the sleep timer:
 * - [Fixed]: Traditional countdown timer with a hard deadline (e.g. stops after 15 min regardless of activity).
 * - [Inactivity]: Smart watchdog timer that resets whenever user touch/scroll activity is detected,
 *   and restores fading audio if the user wakes up and touches the screen.
 */
enum class TimerMode {
    Fixed,
    Inactivity;

    companion object {
        val Default: TimerMode = Inactivity

        fun fromStorage(value: String?): TimerMode =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
