package com.notch.neru.feature.timer.timer

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.notch.neru.core.data.model.MAX_TIMER_MINUTES
import com.notch.neru.core.data.model.ScreenLockMethod
import com.notch.neru.core.data.model.TimerMode
import com.notch.neru.core.data.model.TimerPhase
import com.notch.neru.core.data.model.UserSettings
import com.notch.neru.core.data.repository.SettingsRepository
import com.notch.neru.core.data.repository.TimerRepository
import com.notch.neru.core.data.util.remainingMillisToDisplayMinutes
import com.notch.neru.core.service.SleepTimerService
import com.notch.neru.core.service.screen.AccessibilityLockHelper
import com.notch.neru.core.service.screen.ScreenLockHelper
import com.notch.neru.core.service.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Intent
import android.util.Log
import com.notch.neru.core.service.usage.ForegroundAppDetector

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val settingsRepository: SettingsRepository,
    private val shizukuManager: ShizukuManager,
    private val screenLockHelper: ScreenLockHelper,
    private val accessibilityLockHelper: AccessibilityLockHelper,
    private val foregroundAppDetector: ForegroundAppDetector,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _selectedMinutes = MutableStateFlow(DEFAULT_SELECTED_MINUTES)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val shizukuState: StateFlow<ShizukuManager.State> = shizukuManager.state

    // Guards the startup permission dialog so it fires once per process lifetime.
    // Survives navigation (ViewModel is scoped to the Timer nav entry).
    private var startupCheckDone = false

    init {
        viewModelScope.launch {
            val preset = settingsRepository.settings.first().presetMinutes
            // compareAndSet: don't stomp a value the user already dialed in the
            // brief window before the preset finished loading.
            _selectedMinutes.compareAndSet(DEFAULT_SELECTED_MINUTES, preset)
        }
    }

    val uiState: StateFlow<TimerUiState> = combine(
        timerRepository.timerState,
        _selectedMinutes,
        settingsRepository.settings,
    ) { timerState, selectedMin, userSettings ->
        val activeMode = if (timerState.phase != TimerPhase.IDLE) timerState.mode else userSettings.timerMode
        when (timerState.phase) {
            TimerPhase.IDLE -> {
                TimerUiState.Idle(selectedMinutes = selectedMin, mode = activeMode)
            }
            TimerPhase.RUNNING -> {
                val totalSeconds = (timerState.totalDurationMillis / 1000).toInt()
                val progress = if (timerState.totalDurationMillis > 0L) {
                    timerState.remainingMillis.toFloat() / timerState.totalDurationMillis.toFloat()
                } else {
                    0f
                }
                TimerUiState.Running(
                    totalMinutes = (totalSeconds / 60),
                    remainingMillis = timerState.remainingMillis,
                    progress = progress,
                    mode = activeMode,
                )
            }
            TimerPhase.FADING_OUT -> TimerUiState.FadingOut(
                remainingMillis = timerState.remainingMillis,
                totalDurationMillis = timerState.totalDurationMillis,
                mode = activeMode,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Idle())

    fun getAdminComponent(): ComponentName = screenLockHelper.adminComponent
    fun isDeviceAdminActive(): Boolean = screenLockHelper.isAdminActive()

    fun refreshShizuku() = shizukuManager.refresh()
    fun requestShizukuPermission() = shizukuManager.requestPermission()

    suspend fun computeStartupPermissionCheck(): StartupPermissionCheck? {
        if (startupCheckDone) return null
        startupCheckDone = true
        // Wait for Shizuku's binder to connect — a cold-start `pingBinder()` race
        // would otherwise report NotRunning even when Shizuku is up.
        val shizukuState = shizukuManager.awaitInitialState()
        val s = settingsRepository.settings.first()
        val shizukuReady = shizukuState == ShizukuManager.State.Ready
        val lockMethod = s.screenLockMethod
        val adminMissing = s.screenOff && lockMethod == ScreenLockMethod.DeviceAdmin &&
            !screenLockHelper.isAdminActive()
        val accessibilityMissing = s.screenOff && lockMethod == ScreenLockMethod.Accessibility &&
            !accessibilityLockHelper.isServiceEnabled()
        val shizukuFeatures = buildList {
            if (s.screenOff && lockMethod == ScreenLockMethod.Shizuku && !shizukuReady) {
                add(ShizukuFeature.SCREEN_OFF)
            }
            if (s.turnOffWifi && !shizukuReady) add(ShizukuFeature.WIFI)
            if (s.turnOffBluetooth && !shizukuReady) add(ShizukuFeature.BLUETOOTH)
            if (s.turnOffMobileData && !shizukuReady) add(ShizukuFeature.MOBILE_DATA)
        }
        return StartupPermissionCheck(adminMissing, accessibilityMissing, shizukuFeatures)
    }

    /**
     * Updates the live UI minutes without persisting. Called continuously during a
     * dial drag — persisting every tick would flood DataStore with ~30 writes per drag.
     * Ignored while a timer is running: during-drag previews are driven by dialState
     * in the UI, and updating `_selectedMinutes` would silently shift the idle preset
     * once the running session ends.
     */
    fun setMinutes(minutes: Int) {
        if (timerRepository.timerState.value.phase != TimerPhase.IDLE) return
        _selectedMinutes.value = minutes.coerceIn(1, MAX_TIMER_MINUTES)
    }

    /**
     * Applies a committed minutes value. When idle, persists it as the preset. When a
     * timer is running, dispatches to the service to adjust remaining time.
     */
    fun commitMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(1, MAX_TIMER_MINUTES)
        val state = timerRepository.timerState.value
        when (state.phase) {
            TimerPhase.RUNNING -> {
                if (remainingMillisToDisplayMinutes(state.remainingMillis) == coerced) return
                context.startService(
                    SleepTimerService.intent(context, SleepTimerService.ACTION_SET_MINUTES)
                        .putExtra(SleepTimerService.EXTRA_MINUTES, coerced),
                )
            }
            else -> {
                _selectedMinutes.value = coerced
                viewModelScope.launch {
                    settingsRepository.updatePresetMinutes(coerced)
                }
            }
        }
    }

    fun setTimerMode(mode: TimerMode) {
        viewModelScope.launch {
            settingsRepository.updateTimerMode(mode)
        }
    }

    fun updateStopMediaPlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStopMediaPlayback(enabled)
        }
    }

    fun updateScreenOff(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateScreenOff(enabled)
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean = accessibilityLockHelper.isServiceEnabled()
    fun hasUsagePermission(): Boolean = foregroundAppDetector.hasUsagePermission()

    fun setTargetAllApps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTargetAllApps(enabled)
        }
    }

    fun toggleTargetApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.toggleTargetAppPackage(packageName)
        }
    }

    fun setSelectedTargetApps(packages: Set<String>) {
        viewModelScope.launch {
            val currentCustom = settings.value.customTargetAppPackages
            settingsRepository.updateCustomTargetAppPackages(currentCustom + packages)
            settingsRepository.updateSelectedTargetAppPackages(packages)
            if (packages.isNotEmpty()) {
                settingsRepository.updateTargetAllApps(false)
            } else {
                settingsRepository.updateTargetAllApps(true)
            }
        }
    }

    fun resetInactivityTimer() {
        context.startService(SleepTimerService.intent(context, SleepTimerService.ACTION_I_AM_AWAKE))
    }

    fun startTimer() {
        val minutes = _selectedMinutes.value
        if (minutes <= 0) return
        val s = settings.value
        val packages = s.selectedTargetAppPackages
        if (!s.targetAllApps && packages.isNotEmpty()) {
            val currentFg = foregroundAppDetector.getForegroundPackage()
            val targetPkg = if (currentFg != null && currentFg in packages) currentFg else packages.first()
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                } catch (e: Exception) {
                    Log.w("TimerViewModel", "Could not launch target app", e)
                }
            }
        }
        SleepTimerService.start(context, minutes * 60 * 1000L, TimerMode.Inactivity)
    }

    fun stopTimer() {
        SleepTimerService.cancel(context)
    }

    fun addStep() {
        context.startService(SleepTimerService.intent(context, SleepTimerService.ACTION_ADD_MINUTES))
    }

    fun subtractStep() {
        context.startService(SleepTimerService.intent(context, SleepTimerService.ACTION_SUBTRACT_MINUTES))
    }

    private companion object {
        // Must match UserSettings().presetMinutes so the compareAndSet in init only
        // yields when the user hasn't touched the dial yet.
        const val DEFAULT_SELECTED_MINUTES = 15
    }
}

data class StartupPermissionCheck(
    val adminMissing: Boolean,
    val accessibilityMissing: Boolean,
    val shizukuMissingFeatures: List<ShizukuFeature>,
)

enum class ShizukuFeature { SCREEN_OFF, WIFI, BLUETOOTH, MOBILE_DATA }
