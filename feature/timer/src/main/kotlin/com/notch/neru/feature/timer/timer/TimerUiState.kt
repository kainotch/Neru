package com.notch.neru.feature.timer.timer

import com.notch.neru.core.data.model.TimerMode

sealed interface TimerUiState {

    data class Idle(
        val selectedMinutes: Int = 15,
        val mode: TimerMode = TimerMode.Inactivity,
    ) : TimerUiState

    data class Running(
        val totalMinutes: Int,
        val remainingMillis: Long,
        val progress: Float,
        val mode: TimerMode = TimerMode.Inactivity,
    ) : TimerUiState

    data class FadingOut(
        val remainingMillis: Long = 0L,
        val totalDurationMillis: Long = 0L,
        val mode: TimerMode = TimerMode.Inactivity,
    ) : TimerUiState
}
