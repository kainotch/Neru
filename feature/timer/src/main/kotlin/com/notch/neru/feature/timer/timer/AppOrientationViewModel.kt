package com.notch.neru.feature.timer.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.notch.neru.core.data.model.AutoRotateMode
import com.notch.neru.core.data.model.UserSettings
import com.notch.neru.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Exposes the auto-rotate setting to SleepTimerNavHost, which sits outside any
// NavBackStackEntry and so has no destination-scoped ViewModel of its own.
@HiltViewModel
class AppOrientationViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val autoRotateMode: StateFlow<AutoRotateMode> =
        settingsRepository.settings
            .map { it.autoRotateMode }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                UserSettings().autoRotateMode,
            )
}
