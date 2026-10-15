package com.notch.neru.feature.timer.settings

import com.notch.neru.core.data.model.UserSettings
import com.notch.neru.core.service.shizuku.ShizukuManager

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val shizukuState: ShizukuManager.State = ShizukuManager.State.NotInstalled,
    val isDeviceAdminActive: Boolean = false,
    val isAccessibilityLockEnabled: Boolean = false,
)
