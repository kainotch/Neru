package com.notch.neru.core.data.repository

import com.notch.neru.core.data.model.AutoRotateMode
import com.notch.neru.core.data.model.ScreenLockMethod
import com.notch.neru.core.data.model.ThemeId
import com.notch.neru.core.data.model.TimerMode
import com.notch.neru.core.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateStopMediaPlayback(enabled: Boolean)
    suspend fun updateFadeOutDuration(seconds: Int)
    suspend fun updateScreenOff(enabled: Boolean)
    suspend fun updateScreenLockMethod(method: ScreenLockMethod)
    suspend fun updateTurnOffWifi(enabled: Boolean)
    suspend fun updateTurnOffMobileData(enabled: Boolean)
    suspend fun updateTurnOffBluetooth(enabled: Boolean)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun updateTheme(theme: ThemeId)
    suspend fun updateStarsEnabled(enabled: Boolean)
    suspend fun updateAutoRotateMode(mode: AutoRotateMode)
    suspend fun updateStepMinutes(minutes: Int)
    suspend fun updatePresetMinutes(minutes: Int)
    suspend fun updateLaunchAnimationEnabled(enabled: Boolean)
    suspend fun updateTimerMode(mode: TimerMode)
    suspend fun updateTargetAllApps(enabled: Boolean)
    suspend fun updateSelectedTargetApp(packageName: String?, appName: String?)
    suspend fun updateSelectedTargetAppPackages(packages: Set<String>)
    suspend fun toggleTargetAppPackage(packageName: String)
    suspend fun updateCustomTargetAppPackages(packages: Set<String>)
}
