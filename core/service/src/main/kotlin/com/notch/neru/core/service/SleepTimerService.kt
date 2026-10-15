package com.notch.neru.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import com.notch.neru.core.data.model.MAX_TIMER_MINUTES
import com.notch.neru.core.data.model.ScreenLockMethod
import com.notch.neru.core.data.model.TimerMode
import com.notch.neru.core.data.model.TimerPhase
import com.notch.neru.core.data.model.TimerState
import com.notch.neru.core.data.model.UserSettings
import com.notch.neru.core.data.repository.SettingsRepository
import com.notch.neru.core.data.repository.TimerRepositoryImpl
import com.notch.neru.core.data.util.remainingMillisToDisplayMinutes
import com.notch.neru.core.service.interaction.InteractionDetector
import com.notch.neru.core.service.media.MediaVolumeController
import com.notch.neru.core.service.notification.TimerNotificationManager
import com.notch.neru.core.service.screen.AccessibilityLockHelper
import com.notch.neru.core.service.screen.ScreenLockHelper
import com.notch.neru.core.service.sensor.ShakeDetector
import com.notch.neru.core.service.shizuku.ShizukuBluetoothController
import com.notch.neru.core.service.shizuku.ShizukuManager
import com.notch.neru.core.service.shizuku.ShizukuMobileDataController
import com.notch.neru.core.service.shizuku.ShizukuScreenOffHelper
import com.notch.neru.core.service.shizuku.ShizukuWifiController
import com.notch.neru.core.service.usage.ForegroundAppDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class SleepTimerService : Service() {

    @Inject lateinit var timerRepository: TimerRepositoryImpl
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationManager: TimerNotificationManager
    @Inject lateinit var mediaVolumeController: MediaVolumeController
    @Inject lateinit var screenLockHelper: ScreenLockHelper
    @Inject lateinit var accessibilityLockHelper: AccessibilityLockHelper
    @Inject lateinit var shizukuManager: ShizukuManager
    @Inject lateinit var shizukuScreenOffHelper: ShizukuScreenOffHelper
    @Inject lateinit var shizukuWifiController: ShizukuWifiController
    @Inject lateinit var shizukuMobileDataController: ShizukuMobileDataController
    @Inject lateinit var shizukuBluetoothController: ShizukuBluetoothController
    @Inject lateinit var foregroundAppDetector: ForegroundAppDetector
    @Inject lateinit var timerWidgetRefresher: TimerWidgetRefresher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countdownJob: Job? = null
    private var fadeJob: Job? = null

    private val shakeDetector by lazy {
        ShakeDetector(this) {
            handleUserActivity()
        }
    }

    // Deadline on the elapsedRealtime clock. delay() ticks are uptime-based and pause
    // while the CPU deep-sleeps, so the remaining time must be re-derived from a clock
    // that keeps counting through sleep — otherwise the countdown silently extends.
    private var deadlineElapsed: Long = 0L
    private var remainingMillis: Long = 0L
    private var totalDurationMillis: Long = 0L
    private var stepMinutes: Int = 0
    private var activeTimerMode: TimerMode = TimerMode.Inactivity

    // Bumped by startTimer so an in-flight cancel teardown can tell whether a newer
    // timer took over while it was joining the old countdown job (see cancelTimer).
    private var timerGeneration: Int = 0

    // Last minutes value shown in the notification; notify() is skipped while the
    // displayed text would not change. Int.MIN_VALUE forces the next update through.
    private var lastNotifiedMinutes: Int = Int.MIN_VALUE

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    companion object {
        const val ACTION_START = "com.notch.neru.action.START"
        const val ACTION_CANCEL = "com.notch.neru.action.CANCEL"
        const val ACTION_ADD_MINUTES = "com.notch.neru.action.ADD_MINUTES"
        const val ACTION_SUBTRACT_MINUTES = "com.notch.neru.action.SUBTRACT_MINUTES"
        const val ACTION_SET_MINUTES = "com.notch.neru.action.SET_MINUTES"
        const val ACTION_I_AM_AWAKE = "com.notch.neru.action.I_AM_AWAKE"
        const val EXTRA_DURATION_MILLIS = "com.notch.neru.extra.DURATION_MILLIS"
        const val EXTRA_MINUTES = "com.notch.neru.extra.MINUTES"
        const val EXTRA_TIMER_MODE = "com.notch.neru.extra.TIMER_MODE"
        private const val FADE_IN_SECONDS = 2
        private const val MAX_TIMER_MILLIS = MAX_TIMER_MINUTES * 60_000L
        private const val TAG = "SleepTimerService"

        /**
         * The explicit intent every dispatch surface must use. Kept here so a class
         * or package move breaks callers at compile time in one place instead of as
         * several silent no-op intents.
         */
        fun intent(context: Context, actionName: String): Intent =
            Intent().apply {
                action = actionName
                setClassName(context, SleepTimerService::class.java.name)
            }

        /**
         * Starts the countdown as a foreground service. Returns false when the
         * platform denies the start: a background-restricted app (Settings → "Don't
         * allow background activity") is refused the tap exemption and
         * startForegroundService throws ForegroundServiceStartNotAllowedException
         * (an IllegalStateException) — callers get a logged no-op, not a crash.
         */
        fun start(context: Context, durationMillis: Long, mode: TimerMode? = null): Boolean =
            try {
                val intent = intent(context, ACTION_START)
                    .putExtra(EXTRA_DURATION_MILLIS, durationMillis)
                if (mode != null) {
                    intent.putExtra(EXTRA_TIMER_MODE, mode.name)
                }
                context.startForegroundService(intent)
                true
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Foreground service start denied", e)
                false
            }

        /** Cancels a running countdown; a stale call is absorbed by the stale-intent guard. */
        fun cancel(context: Context) {
            context.startService(intent(context, ACTION_CANCEL))
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Prime synchronously so the first notification uses the saved step,
        // not a default from before the settings flow has emitted.
        stepMinutes = runBlocking { settingsRepository.settings.first().stepMinutes }
        serviceScope.launch {
            settingsRepository.settings
                .map { it.stepMinutes }
                .collect { newStep ->
                    val changed = newStep != stepMinutes
                    stepMinutes = newStep
                    // Keep the +/− action labels current if the step is edited mid-timer.
                    if (changed && countdownJob?.isActive == true && lastNotifiedMinutes >= 0) {
                        notificationManager.updateNotification(lastNotifiedMinutes, stepMinutes)
                    }
                }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        // If the service was restarted by the OS (or by a stale PendingIntent from a
        // notification that survived process death) for any action other than START,
        // there is no countdown to modify. Skip straight to stopSelf — otherwise we
        // would never call startForeground within the 5-second window and crash with
        // ForegroundServiceDidNotStartInTimeException.
        if (action != ACTION_START && countdownJob?.isActive != true) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_START -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)
                if (durationMillis > 0) {
                    val modeString = intent.getStringExtra(EXTRA_TIMER_MODE)
                    val mode = if (modeString != null) {
                        TimerMode.fromStorage(modeString)
                    } else {
                        runBlocking { settingsRepository.settings.first().timerMode }
                    }
                    startTimer(durationMillis.coerceAtMost(MAX_TIMER_MILLIS), mode)
                } else if (countdownJob?.isActive != true) {
                    // Invalid start with nothing running — same 5-second-window
                    // consideration as the stale-intent guard above.
                    stopSelf(startId)
                }
            }
            ACTION_ADD_MINUTES -> {
                addStep()
            }
            ACTION_SUBTRACT_MINUTES -> {
                subtractStep()
            }
            ACTION_SET_MINUTES -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
                if (minutes > 0) setRemainingMinutes(minutes)
            }
            ACTION_I_AM_AWAKE -> {
                handleUserActivity()
            }
            ACTION_CANCEL -> {
                cancelTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(durationMillis: Long, mode: TimerMode) {
        timerGeneration++
        val previousJob = countdownJob
        activeTimerMode = mode

        totalDurationMillis = durationMillis
        setRemaining(durationMillis)

        InteractionDetector.setInteractionListener { handleUserActivity() }

        // Create notification channel and start foreground
        notificationManager.createNotificationChannel()
        lastNotifiedMinutes = remainingMillisToDisplayMinutes(remainingMillis)
        val notification = notificationManager.buildNotification(
            remainingMinutes = lastNotifiedMinutes,
            stepMinutes = stepMinutes,
            phase = TimerPhase.RUNNING,
            mode = activeTimerMode,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                TimerNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
        }

        // Update repository
        updateTimerState(TimerPhase.RUNNING)

        // Start countdown. onTimerExpired runs inside this job so that cancelling
        // the job also cancels the fade-out — lets + and Cancel interrupt the fade.
        countdownJob = serviceScope.launch {
            // If the previous countdown was mid-fade, stop it and undo its volume
            // ramp before this timer takes over — otherwise media would stay quiet
            // and the faded level would be captured as the new "original" volume.
            previousJob?.cancelAndJoin()
            mediaVolumeController.restoreVolume()
            runCountdownAndExpire()
        }
    }

    /**
     * Handles debounced user interaction (touch or scroll) detected across other apps.
     * In Inactivity mode:
     * - If in FADING_OUT phase ("Wait, I'm still awake!"): cancels fade, restores full volume,
     *   resets remaining time back to full duration, and continues countdown.
     * - If in RUNNING phase: resets the inactivity deadline back to full duration.
     */
    private fun handleUserActivity() {
        shakeDetector.stop()
        if (activeTimerMode != TimerMode.Inactivity) return
        serviceScope.launch {
            val currentState = timerRepository.timerState.value
            when (currentState.phase) {
                TimerPhase.FADING_OUT -> {
                    // Smart Fade-Out Recovery ("Wait, I'm still awake!")
                    fadeJob?.cancelAndJoin()
                    fadeJob = null
                    countdownJob?.cancelAndJoin()
                    mediaVolumeController.restoreVolume()
                    setRemaining(totalDurationMillis)
                    updateTimerState(TimerPhase.RUNNING)
                    lastNotifiedMinutes = remainingMillisToDisplayMinutes(remainingMillis)
                    notificationManager.updateNotification(
                        remainingMinutes = lastNotifiedMinutes,
                        stepMinutes = stepMinutes,
                        phase = TimerPhase.RUNNING,
                        mode = activeTimerMode,
                        userJustActive = true,
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SleepTimerService,
                            getString(R.string.timer_reset_we_heard_you),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    countdownJob = serviceScope.launch {
                        runCountdownAndExpire()
                    }
                }
                TimerPhase.RUNNING -> {
                    // Reset inactivity timer back to full duration on touch/scroll
                    setRemaining(totalDurationMillis)
                    updateTimerState(TimerPhase.RUNNING)
                    lastNotifiedMinutes = remainingMillisToDisplayMinutes(remainingMillis)
                    notificationManager.updateNotification(
                        remainingMinutes = lastNotifiedMinutes,
                        stepMinutes = stepMinutes,
                        phase = TimerPhase.RUNNING,
                        mode = activeTimerMode,
                        userJustActive = true,
                    )
                }
                TimerPhase.IDLE -> {}
            }
        }
    }

    /** Anchors [remainingMillis] to a fresh elapsedRealtime deadline. */
    private fun setRemaining(millis: Long) {
        remainingMillis = millis.coerceAtLeast(0L)
        deadlineElapsed = SystemClock.elapsedRealtime() + remainingMillis
    }

    private suspend fun runCountdownAndExpire() {
        val settings = settingsRepository.settings.first()
        val rawFadeMillis = if (settings.stopMediaPlayback) settings.fadeOutDurationSeconds * 1000L else 0L
        val fadeDurationMillis = minOf(rawFadeMillis, totalDurationMillis / 2)
        val fadeSeconds = (fadeDurationMillis / 1000L).toInt().coerceAtLeast(1)
        val targetAppPackages = if (!settings.targetAllApps) {
            if (settings.selectedTargetAppPackages.isNotEmpty()) {
                settings.selectedTargetAppPackages
            } else {
                val legacy = settings.selectedTargetAppPackage
                if (legacy != null) setOf(legacy) else emptySet()
            }
        } else {
            emptySet()
        }

        while (true) {
            // When bound to specific apps, only count down while any of those apps is visible on the screen.
            // If the user closes or leaves all of those apps, reset/hold the timer until they reopen one.
            if (targetAppPackages.isNotEmpty() && foregroundAppDetector.hasUsagePermission()) {
                val currentFg = foregroundAppDetector.getForegroundPackage()
                if (currentFg == null || currentFg !in targetAppPackages) {
                    setRemaining(totalDurationMillis)
                    updateTimerState(TimerPhase.RUNNING)
                    delay(1_000L)
                    continue
                }
            }

            remainingMillis = (deadlineElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

            // For Inactivity mode: enter the fade window while allowing wake-up recovery on touch
            if (activeTimerMode == TimerMode.Inactivity &&
                fadeDurationMillis > 0L &&
                remainingMillis <= fadeDurationMillis &&
                remainingMillis > 0L &&
                timerRepository.timerState.value.phase != TimerPhase.FADING_OUT
            ) {
                updateTimerState(TimerPhase.FADING_OUT)
                lastNotifiedMinutes = Int.MIN_VALUE
                notificationManager.updateNotification(
                    remainingMinutes = remainingMillisToDisplayMinutes(remainingMillis),
                    stepMinutes = stepMinutes,
                    phase = TimerPhase.FADING_OUT,
                    mode = activeTimerMode,
                )
                shakeDetector.start()
                fadeJob = serviceScope.launch {
                    mediaVolumeController.fadeOutAndPause(fadeSeconds)
                }
            } else if (timerRepository.timerState.value.phase == TimerPhase.FADING_OUT) {
                updateTimerState(TimerPhase.FADING_OUT)
            } else {
                updateTimerState(TimerPhase.RUNNING)
                notifyRemainingIfChanged()
            }

            if (remainingMillis <= 0L) break
            delay(minOf(1_000L, remainingMillis))
        }
        shakeDetector.stop()
        onTimerExpired()
    }

    private fun notifyRemainingIfChanged(userJustActive: Boolean = false) {
        val minutes = remainingMillisToDisplayMinutes(remainingMillis)
        if (!userJustActive && minutes == lastNotifiedMinutes) return
        lastNotifiedMinutes = minutes
        notificationManager.updateNotification(
            remainingMinutes = minutes,
            stepMinutes = stepMinutes,
            phase = timerRepository.timerState.value.phase,
            mode = activeTimerMode,
            userJustActive = userJustActive,
        )
    }

    private fun addStep() {
        when (timerRepository.timerState.value.phase) {
            TimerPhase.RUNNING -> {
                if (countdownJob?.isActive != true) return
                // Already at 0 and the job is mid-teardown in onTimerExpired (no fade,
                // phase still RUNNING): adding here would set a deadline no loop reads,
                // and the teardown then resets to IDLE — the tap would be silently lost.
                if (remainingMillis <= 0L) return
                val stepMillis = stepMinutes * 60_000L
                setRemaining((remainingMillis + stepMillis).coerceAtMost(MAX_TIMER_MILLIS))
                totalDurationMillis = (totalDurationMillis + stepMillis).coerceAtMost(MAX_TIMER_MILLIS)
                updateTimerState(TimerPhase.RUNNING)
                notifyRemainingIfChanged()
            }
            TimerPhase.FADING_OUT -> {
                // Replace countdownJob with the fade-in + restart so Cancel during
                // the 2 s fade-in window cancels this whole sequence, not just the
                // already-finished fade-out. The countdown and fade-in run in
                // parallel — the clock ticks from the moment the user presses +,
                // not after the fade-in completes.
                val oldJob = countdownJob ?: return
                val stepMillis = (stepMinutes * 60_000L).coerceAtMost(MAX_TIMER_MILLIS)
                countdownJob = serviceScope.launch {
                    oldJob.cancelAndJoin()
                    totalDurationMillis = stepMillis
                    setRemaining(stepMillis)
                    updateTimerState(TimerPhase.RUNNING)
                    notifyRemainingIfChanged()
                    launch { mediaVolumeController.fadeInToOriginal(FADE_IN_SECONDS) }
                    runCountdownAndExpire()
                }
            }
            else -> {}
        }
    }

    private fun setRemainingMinutes(minutes: Int) {
        if (timerRepository.timerState.value.phase != TimerPhase.RUNNING) return
        if (countdownJob?.isActive != true) return
        // Ignore a dial-commit that lands in the post-expiry teardown window (see addStep).
        if (remainingMillis <= 0L) return
        setRemaining(minutes.coerceIn(1, MAX_TIMER_MINUTES) * 60_000L)
        totalDurationMillis = maxOf(totalDurationMillis, remainingMillis)
        updateTimerState(TimerPhase.RUNNING)
        notifyRemainingIfChanged()
    }

    private fun subtractStep() {
        if (timerRepository.timerState.value.phase != TimerPhase.RUNNING) return
        if (countdownJob?.isActive != true) return
        val stepMillis = stepMinutes * 60_000L
        if (remainingMillis <= stepMillis) return
        setRemaining(remainingMillis - stepMillis)
        totalDurationMillis = (totalDurationMillis - stepMillis).coerceAtLeast(remainingMillis)
        updateTimerState(TimerPhase.RUNNING)
        notifyRemainingIfChanged()
    }

    private fun cancelTimer() {
        val job = countdownJob
        countdownJob = null
        val currentFadeJob = fadeJob
        fadeJob = null
        val generation = timerGeneration

        // Clean up interaction watching
        shakeDetector.stop()
        InteractionDetector.setInteractionListener(null)

        serviceScope.launch {
            // NonCancellable: a second ACTION_CANCEL landing mid-teardown trips the
            // stale-intent guard's stopSelf → onDestroy → serviceScope.cancel(),
            // which would otherwise kill this coroutine before the volume restore
            // and the IDLE write — stranding a phantom "running" phase that the
            // tile, widget, and app would keep rendering until process death.
            withContext(NonCancellable) {
                // Join the fade-out before restoring volume, otherwise the still-running
                // fade coroutine would overwrite the restored volume at its next step.
                currentFadeJob?.cancelAndJoin()
                job?.cancelAndJoin()
                mediaVolumeController.restoreVolume()
                // A newer timer may have started while the join was in flight — its
                // foreground session must survive this teardown.
                if (timerGeneration != generation) return@withContext
                updateTimerState(TimerPhase.IDLE)
                // Push the idle draw to the widgets before we let the process go: the
                // in-process observer's render could be lost if we're reclaimed right after.
                timerWidgetRefresher.refresh()
                notificationManager.cancelNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun onTimerExpired() {
        val settings: UserSettings = settingsRepository.settings.first()

        // Clean up interaction watching
        shakeDetector.stop()
        InteractionDetector.setInteractionListener(null)

        if (settings.stopMediaPlayback) {
            if (timerRepository.timerState.value.phase != TimerPhase.FADING_OUT) {
                updateTimerState(TimerPhase.FADING_OUT)
                lastNotifiedMinutes = Int.MIN_VALUE
                notificationManager.updateNotification(
                    remainingMinutes = 0,
                    stepMinutes = stepMinutes,
                    phase = TimerPhase.FADING_OUT,
                    mode = activeTimerMode,
                )
                mediaVolumeController.fadeOutAndPause(settings.fadeOutDurationSeconds)
            } else {
                mediaVolumeController.pauseMedia()
                mediaVolumeController.restoreVolume()
            }
        }

        if (settings.turnOffWifi && shizukuManager.isReady()) {
            shizukuWifiController.disableWifi()
        }

        if (settings.turnOffBluetooth && shizukuManager.isReady()) {
            shizukuBluetoothController.disableBluetooth()
        }

        if (settings.turnOffMobileData && shizukuManager.isReady()) {
            shizukuMobileDataController.disableMobileData()
        }

        if (settings.screenOff) {
            when (settings.screenLockMethod) {
                ScreenLockMethod.Shizuku -> {
                    if (shizukuManager.isReady()) {
                        // KEYCODE_POWER is a toggle: only send it while the screen is
                        // on, otherwise "turn off the screen" would wake a sleeping
                        // device. If the screen is already off there is nothing to do —
                        // falling back to the hard lock here would defeat the user's
                        // choice to keep biometric unlock working.
                        if (powerManager.isInteractive && !shizukuScreenOffHelper.turnOffScreen()) {
                            screenLockHelper.lockScreen()
                        }
                    } else {
                        // Shizuku chosen but unavailable — fall back to the hard lock.
                        screenLockHelper.lockScreen()
                    }
                }
                ScreenLockMethod.Accessibility -> {
                    // GLOBAL_ACTION_LOCK_SCREEN locks like the power button and is
                    // idempotent on an already-locked device — no isInteractive gate
                    // needed. Fall back to the hard lock if the accessibility service
                    // was disabled (or the device runs API < 28).
                    if (!accessibilityLockHelper.lockScreen()) {
                        screenLockHelper.lockScreen()
                    }
                }
                ScreenLockMethod.DeviceAdmin -> {
                    // Hard-lock: forces credential on next unlock.
                    screenLockHelper.lockScreen()
                }
            }
        }

        // "+" during the fade replaces countdownJob with a successor that restarts
        // the countdown; if that happened, this coroutine must not tear down the
        // foreground session the successor is still using.
        if (countdownJob !== coroutineContext[Job]) return
        countdownJob = null

        // Reset state before stopping foreground to avoid race with onDestroy
        timerRepository.updateState(TimerState(mode = activeTimerMode))
        // Push the idle draw to the widgets before we let the process go: the
        // in-process observer's render could be lost if we're reclaimed right after.
        timerWidgetRefresher.refresh()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateTimerState(phase: TimerPhase) {
        timerRepository.updateState(
            TimerState(
                phase = phase,
                totalDurationMillis = totalDurationMillis,
                remainingMillis = remainingMillis,
                mode = activeTimerMode,
            ),
        )
    }

    override fun onDestroy() {
        shakeDetector.stop()
        InteractionDetector.setInteractionListener(null)
        serviceScope.cancel()
        super.onDestroy()
    }
}
