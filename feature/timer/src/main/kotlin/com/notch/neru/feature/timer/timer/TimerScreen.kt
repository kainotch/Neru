package com.notch.neru.feature.timer.timer

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notch.neru.feature.timer.timer.components.PresetDurationGrid
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notch.neru.core.data.model.MAX_TIMER_MINUTES
import com.notch.neru.core.data.util.isSystemReduceMotionEnabled
import com.notch.neru.core.data.util.remainingMillisToDisplayMinutes
import com.notch.neru.core.service.shizuku.ShizukuManager
import com.notch.neru.feature.timer.R
import com.notch.neru.feature.timer.settings.components.AccessibilityRequiredDialog
import com.notch.neru.feature.timer.settings.components.DeviceAdminRequiredDialog
import com.notch.neru.feature.timer.settings.components.ShizukuRequiredDialog
import com.notch.neru.feature.timer.theme.AppThemes
import com.notch.neru.feature.timer.theme.ProvideAppTheme
import com.notch.neru.feature.timer.theme.appTheme
import com.notch.neru.feature.timer.theme.rememberAnimatedAppTheme
import com.notch.neru.core.data.model.ScreenLockMethod
import com.notch.neru.core.data.model.TimerMode
import com.notch.neru.feature.timer.timer.components.AppPickerDialog
import com.notch.neru.feature.timer.timer.components.CircularDial
import com.notch.neru.feature.timer.timer.components.LaunchOverlay
import com.notch.neru.feature.timer.timer.components.LaunchPhase
import com.notch.neru.feature.timer.timer.components.TargetAppDefaults
import com.notch.neru.feature.timer.timer.components.TargetAppsGrid
import com.notch.neru.feature.timer.timer.components.TimeDisplay
import com.notch.neru.feature.timer.timer.components.TimerBackground
import com.notch.neru.feature.timer.timer.components.rememberCircularDialState
import com.notch.neru.feature.timer.timer.components.rememberLaunchAnimationController
import kotlin.math.atan2

private const val ROTATION_DURATION_MS = 350

@Composable
fun TimerScreen(
    onNavigateToSettings: (highlightScreenOff: Boolean) -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val animatedTheme = rememberAnimatedAppTheme(AppThemes.byId(settings.theme))
    ProvideAppTheme(animatedTheme) {
        TimerContent(
            onNavigateToSettings = onNavigateToSettings,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun TimerContent(
    onNavigateToSettings: (highlightScreenOff: Boolean) -> Unit,
    viewModel: TimerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dialState = rememberCircularDialState()
    val context = LocalContext.current
    val screenDescription = stringResource(R.string.screen_description)

    val orientation by rememberDeviceOrientation(mode = settings.autoRotateMode)
    val isLandscape = orientation == DeviceOrientation.LANDSCAPE_LEFT ||
        orientation == DeviceOrientation.LANDSCAPE_RIGHT
    val animatedAngle = animatedRotationAngle(orientation)

    val launchController = rememberLaunchAnimationController()
    var buttonCenter by remember { mutableStateOf(Offset.Zero) }
    var dialCenter by remember { mutableStateOf(Offset.Zero) }

    // Snap zurück auf Idle, wenn die App in den Hintergrund geht.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        launchController.reset()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.startTimer()
    }

    var showAdminStartupDialog by remember { mutableStateOf(false) }
    var showAccessibilityStartupDialog by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showUsageAccessDialog by remember { mutableStateOf(false) }
    var shizukuStartupFeatures by remember { mutableStateOf<List<ShizukuFeature>>(emptyList()) }
    val shizukuState by viewModel.shizukuState.collectAsStateWithLifecycle()

    val adminStartupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        showAdminStartupDialog = false
    }

    LaunchedEffect(Unit) {
        viewModel.computeStartupPermissionCheck()?.let { check ->
            showAdminStartupDialog = check.adminMissing
            showAccessibilityStartupDialog = check.accessibilityMissing
            shizukuStartupFeatures = check.shizukuMissingFeatures
        }
    }

    // Auto-close the Shizuku startup dialog once permission is granted.
    LaunchedEffect(shizukuState) {
        if (shizukuStartupFeatures.isNotEmpty() && shizukuState == ShizukuManager.State.Ready) {
            shizukuStartupFeatures = emptyList()
        }
    }

    if (showAdminStartupDialog) {
        DeviceAdminRequiredDialog(
            onRequestPermission = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        viewModel.getAdminComponent(),
                    )
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        screenDescription,
                    )
                }
                adminStartupLauncher.launch(intent)
            },
            onDismiss = { showAdminStartupDialog = false },
        )
    }

    if (showAccessibilityStartupDialog) {
        AccessibilityRequiredDialog(
            onOpenSettings = {
                showAccessibilityStartupDialog = false
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS),
                )
            },
            onDismiss = { showAccessibilityStartupDialog = false },
            dismissLabelRes = R.string.dialog_action_ignore,
        )
    }

    if (shizukuStartupFeatures.isNotEmpty()) {
        val explanations = shizukuStartupFeatures.map { feature ->
            when (feature) {
                ShizukuFeature.SCREEN_OFF -> stringResource(R.string.shizuku_feature_label_display)
                ShizukuFeature.WIFI -> stringResource(R.string.shizuku_feature_label_wifi)
                ShizukuFeature.BLUETOOTH -> stringResource(R.string.shizuku_feature_label_bluetooth)
                ShizukuFeature.MOBILE_DATA -> stringResource(R.string.shizuku_feature_label_mobile_data)
            }
        }
        ShizukuRequiredDialog(
            state = shizukuState,
            featureExplanations = explanations,
            introText = stringResource(R.string.shizuku_startup_intro),
            dismissLabelRes = R.string.dialog_action_ignore,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onDismiss = { shizukuStartupFeatures = emptyList() },
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            selectedPackages = settings.selectedTargetAppPackages,
            onDismiss = { showAppPicker = false },
            onDone = { newSelected ->
                viewModel.setSelectedTargetApps(newSelected)
                if (newSelected.isNotEmpty() && !viewModel.hasUsagePermission()) {
                    showUsageAccessDialog = true
                }
            },
        )
    }

    if (showUsageAccessDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUsageAccessDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.usage_access_dialog_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.usage_access_dialog_desc),
                    color = Color(0xFFC5C9D3),
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showUsageAccessDialog = false
                        context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                ) {
                    Text(
                        text = stringResource(R.string.usage_access_action_grant),
                        color = Color(0xFFC9B8FF),
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showUsageAccessDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_action_ignore),
                        color = Color(0xFF8F96A3),
                    )
                }
            },
            containerColor = Color(0xFF1E222B),
            shape = RoundedCornerShape(16.dp),
        )
    }

    val isRunning = uiState is TimerUiState.Running || uiState is TimerUiState.FadingOut

    LaunchedEffect(uiState) {
        if (uiState is TimerUiState.Idle) {
            val idle = uiState as TimerUiState.Idle
            if (dialState.totalMinutes != idle.selectedMinutes) {
                dialState.setMinutes(idle.selectedMinutes)
            }
        }
    }

    val runningMinutes: Float = when (val s = uiState) {
        is TimerUiState.Running -> s.remainingMillis / 60_000f
        is TimerUiState.FadingOut -> s.remainingMillis / 60_000f
        else -> 0f
    }

    val displayMinutes: Int = if (dialState.isDragging) {
        dialState.totalMinutes
    } else when (val s = uiState) {
        is TimerUiState.Idle -> s.selectedMinutes
        is TimerUiState.Running -> remainingMillisToDisplayMinutes(s.remainingMillis)
        is TimerUiState.FadingOut -> remainingMillisToDisplayMinutes(s.remainingMillis)
    }

    TimerBackground(
        animating = isRunning,
        starsEnabled = settings.starsEnabled,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTopBar(
                onOpenSettings = { onNavigateToSettings(false) },
                iconRotation = animatedAngle,
            )

            Spacer(modifier = Modifier.height(6.dp))

            TargetAppsGrid(
                targetAllApps = settings.targetAllApps,
                selectedPackages = settings.selectedTargetAppPackages,
                customPackages = settings.customTargetAppPackages,
                onToggleAllApps = { enabled ->
                    viewModel.setTargetAllApps(enabled)
                },
                onToggleApp = { pkg ->
                    viewModel.toggleTargetApp(pkg)
                    if (!viewModel.hasUsagePermission()) {
                        showUsageAccessDialog = true
                    }
                },
                onOpenPicker = {
                    showAppPicker = true
                },
                modifier = Modifier.graphicsLayer { rotationZ = animatedAngle },
            )

            val isFadingOut = uiState is TimerUiState.FadingOut
            val selectedPackages = settings.selectedTargetAppPackages
            val isAppSpecific = !settings.targetAllApps && selectedPackages.isNotEmpty()
            val selectedCount = selectedPackages.size

            val appNames = remember(selectedPackages) {
                TargetAppDefaults.DEFAULT_APPS
                    .filter { it.packageName in selectedPackages }
                    .map { it.name }
            }

            val appStatusString = when {
                selectedCount == 1 -> {
                    val firstName = appNames.firstOrNull() ?: settings.selectedTargetAppName ?: "Selected App"
                    stringResource(R.string.target_app_running_status, firstName, displayMinutes)
                }
                selectedCount == 2 && appNames.size >= 2 -> {
                    stringResource(R.string.target_apps_two_status, appNames[0], appNames[1], displayMinutes)
                }
                else -> {
                    stringResource(R.string.target_apps_multiple_status, selectedCount, displayMinutes)
                }
            }

            val endetText: String? = when {
                displayMinutes <= 0 && uiState !is TimerUiState.Running && !isFadingOut -> null
                isFadingOut -> stringResource(R.string.inactivity_checkin_awake_prompt)
                isRunning -> {
                    if (isAppSpecific) {
                        appStatusString
                    } else {
                        stringResource(R.string.inactivity_active_status, displayMinutes)
                    }
                }
                else -> {
                    if (isAppSpecific) {
                        appStatusString
                    } else {
                        stringResource(R.string.inactivity_locks_after, displayMinutes)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .graphicsLayer { rotationZ = animatedAngle }
                    .then(
                        if (isFadingOut) {
                            Modifier.clickable { viewModel.resetInactivityTimer() }
                        } else Modifier
                    )
                    .onGloballyPositioned { coords ->
                        dialCenter = coords.boundsInRoot().center
                    },
                contentAlignment = Alignment.Center,
            ) {
                CircularDial(
                    state = dialState,
                    isRunning = isRunning,
                    runningMinutes = runningMinutes,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onMinutesChanged = viewModel::setMinutes,
                    onMinutesCommitted = viewModel::commitMinutes,
                    impactPulse = { launchController.impactPulse.value },
                    modifier = Modifier.fillMaxSize(),
                )

                TimeDisplay(
                    totalMinutes = displayMinutes,
                    remainingMillis = when (val s = uiState) {
                        is TimerUiState.Running -> s.remainingMillis
                        is TimerUiState.FadingOut -> s.remainingMillis
                        else -> null
                    },
                )
            }

            if (endetText != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = endetText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Color(0xFF8F96A3),
                    modifier = Modifier.graphicsLayer { rotationZ = animatedAngle },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Preset Duration Grid (5, 10, 30, 45 min)
            PresetDurationGrid(
                selectedMinutes = displayMinutes,
                onPresetSelected = { minutes ->
                    if (isRunning) {
                        viewModel.commitMinutes(minutes)
                    } else {
                        dialState.setMinutes(minutes)
                        viewModel.commitMinutes(minutes)
                    }
                },
                enabled = true,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 340.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            val runningRemainingSeconds: Int = when (val s = uiState) {
                is TimerUiState.Running -> (s.remainingMillis / 1000L).toInt()
                else -> 0
            }

            val launchPhase = launchController.phase
            // Der Button zeigt die Running-Visuals (Stop-Icon + Shape-Morph) erst NACHDEM
            // die Launch-Animation komplett durchgelaufen ist. Während der Flug läuft,
            // versteckt der Button sein Play-Icon (hidePlayIcon) — sonst sieht es aus
            // als käme ein zweites Icon hinten aus dem Button raus. Sobald der Controller
            // wieder auf Idle steht (nach der Impact-Phase) und der Timer tatsächlich
            // läuft, wechselt der Button via Crossfade auf Stop.
            val playButtonShowsRunning = isRunning && launchPhase == LaunchPhase.Idle

            ActionRow(
                playButtonShowsRunning = playButtonShowsRunning,
                playButtonDescribesRunning = isRunning || launchPhase != LaunchPhase.Idle,
                hidePlayIcon = launchPhase != LaunchPhase.Idle,
                hapticEnabled = settings.hapticFeedbackEnabled,
                iconRotation = animatedAngle,
                onToggle = {
                    val animating = launchController.phase != LaunchPhase.Idle
                    if (isFadingOut) {
                        viewModel.resetInactivityTimer()
                    } else if (isRunning || animating) {
                        viewModel.stopTimer()
                    } else if (!settings.screenOff) {
                        onNavigateToSettings(true)
                    } else if (settings.screenLockMethod == ScreenLockMethod.DeviceAdmin && !viewModel.isDeviceAdminActive()) {
                        showAdminStartupDialog = true
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startTimer()
                        if (settings.launchAnimationEnabled &&
                            !isSystemReduceMotionEnabled(context) &&
                            buttonCenter != Offset.Zero &&
                            dialCenter != Offset.Zero
                        ) {
                            val dx = dialCenter.x - buttonCenter.x
                            val dy = dialCenter.y - buttonCenter.y
                            launchController.launch(
                                startIconRotationDeg = animatedAngle,
                                targetIconRotationDeg =
                                    Math.toDegrees(atan2(dy, dx).toDouble()).toFloat(),
                            )
                        }
                    }
                },
                onMinusStep = {
                    if (isRunning) {
                        viewModel.subtractStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = ((current - 1).coerceAtLeast(0) / step) * step
                        viewModel.commitMinutes(next)
                    }
                },
                onPlusStep = {
                    if (isRunning) {
                        viewModel.addStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = (current / step + 1) * step
                        viewModel.commitMinutes(next.coerceAtMost(MAX_TIMER_MINUTES))
                    }
                },
                isMinusEnabled = if (isRunning) {
                    runningRemainingSeconds > settings.stepMinutes * 60
                } else {
                    dialState.totalMinutes > 1
                },
                isPlusEnabled = if (isRunning) {
                    runningRemainingSeconds < MAX_TIMER_MINUTES * 60
                } else {
                    dialState.totalMinutes < MAX_TIMER_MINUTES
                },
                buttonScale = { launchController.buttonScale.value },
                onButtonPositioned = { buttonCenter = it },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 340.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Landscape title: hidden to keep clean unified top bar
        AnimatedVisibility(
            visible = false,
            enter = fadeIn(animationSpec = tween(ROTATION_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(ROTATION_DURATION_MS)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 56.dp, end = 8.dp)
                .width(44.dp)
                .height(160.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = appTheme().textPrimary,
                    softWrap = false,
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer { rotationZ = animatedAngle },
                )
            }
        }

        LaunchOverlay(
            controller = launchController,
            buttonCenter = buttonCenter,
            dialCenter = dialCenter,
        )
    }
}

@Composable
private fun animatedRotationAngle(orientation: DeviceOrientation): Float {
    // Shortest-path guard: when the bucket jumps, pick the equivalent target (±360°)
    // closest to the previous value so the animation continues rotating rather than
    // reversing through 0°.
    val continuousTarget = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(orientation) {
        val raw = orientation.counterRotationDegrees()
        val last = continuousTarget.floatValue
        val candidates = listOf(raw, raw + 360f, raw - 360f)
        continuousTarget.floatValue = candidates.minBy { kotlin.math.abs(it - last) }
    }
    val animated by animateFloatAsState(
        targetValue = continuousTarget.floatValue,
        animationSpec = tween(ROTATION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "rotationAngle",
    )
    return animated
}

@Composable
private fun HomeTopBar(
    onOpenSettings: () -> Unit,
    iconRotation: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.screen_off_timer_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
            ),
            color = Color.White,
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = Color(0xFF8F96A3),
                modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
            )
        }
    }
}

@Composable
private fun ActionRow(
    playButtonShowsRunning: Boolean,
    playButtonDescribesRunning: Boolean,
    hidePlayIcon: Boolean,
    hapticEnabled: Boolean,
    iconRotation: Float,
    onToggle: () -> Unit,
    onMinusStep: () -> Unit,
    onPlusStep: () -> Unit,
    isMinusEnabled: Boolean,
    isPlusEnabled: Boolean,
    buttonScale: () -> Float,
    onButtonPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(12.dp)
    val buttonBg = Color(0xFF1E222B)

    val playStopHaptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    val stepHaptic = HapticFeedbackConstants.CLOCK_TICK

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Minus Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(shape)
                .background(buttonBg)
                .clickable(enabled = isMinusEnabled) {
                    if (hapticEnabled) {
                        view.performHapticFeedback(stepHaptic)
                    }
                    onMinusStep()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_step_minus),
                tint = Color.White.copy(alpha = if (isMinusEnabled) 0.85f else 0.3f),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = iconRotation },
            )
        }

        // Play/Stop Button
        val description = stringResource(
            if (playButtonDescribesRunning) R.string.stop_timer else R.string.start_timer,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .graphicsLayer {
                    val scale = buttonScale()
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(buttonBg)
                .clickable {
                    if (hapticEnabled) {
                        view.performHapticFeedback(playStopHaptic)
                    }
                    onToggle()
                }
                .onGloballyPositioned { coords ->
                    onButtonPositioned(coords.boundsInRoot().center)
                },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = playButtonShowsRunning,
                animationSpec = tween(durationMillis = 180),
                label = "playIcon",
            ) { running ->
                val icon: ImageVector = if (running) Icons.Default.Stop else Icons.Default.PlayArrow
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation
                            alpha = if (!running && hidePlayIcon) 0f else 1f
                        },
                )
            }
        }

        // Plus Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(shape)
                .background(buttonBg)
                .clickable(enabled = isPlusEnabled) {
                    if (hapticEnabled) {
                        view.performHapticFeedback(stepHaptic)
                    }
                    onPlusStep()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_step_plus),
                tint = Color.White.copy(alpha = if (isPlusEnabled) 0.85f else 0.3f),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = iconRotation },
            )
        }
    }
}
