package com.notch.neru.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.notch.neru.feature.timer.about.AboutScreen
import com.notch.neru.feature.timer.settings.SettingsScreen
import com.notch.neru.feature.timer.timer.AppOrientationController
import com.notch.neru.feature.timer.timer.AppOrientationViewModel
import com.notch.neru.feature.timer.timer.DeviceOrientation
import com.notch.neru.feature.timer.timer.TimerScreen
import com.notch.neru.feature.timer.timer.rememberDeviceOrientation

@Composable
fun SleepTimerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTimer = backStackEntry?.destination?.hasRoute<TimerRoute>() ?: true

    // Skip the cross-screen fade when the device is tilted: the orientation
    // flip that accompanies Timer ↔ Settings navigation combined with a fade
    // briefly shows Timer's counter-rotated content being rotated by the
    // window manager, appearing upside-down. In natural portrait there is no
    // orientation flip, so the fade is kept.
    val autoRotateMode by hiltViewModel<AppOrientationViewModel>()
        .autoRotateMode.collectAsStateWithLifecycle()
    val deviceOrientation by rememberDeviceOrientation(mode = autoRotateMode)
    val animate = deviceOrientation == DeviceOrientation.PORTRAIT

    AppOrientationController(orientation = deviceOrientation, lockPortrait = isTimer)
    NavHost(
        navController = navController,
        startDestination = TimerRoute,
        enterTransition = { if (animate) fadeIn() else EnterTransition.None },
        exitTransition = { if (animate) fadeOut() else ExitTransition.None },
        popEnterTransition = { if (animate) fadeIn() else EnterTransition.None },
        popExitTransition = { if (animate) fadeOut() else ExitTransition.None },
    ) {
        composable<TimerRoute> {
            TimerScreen(
                onNavigateToSettings = { highlightScreenOff ->
                    navController.navigate(SettingsRoute(highlightScreenOff = highlightScreenOff))
                },
            )
        }
        composable<SettingsRoute> { entry ->
            val route = entry.toRoute<SettingsRoute>()
            SettingsScreen(
                highlightScreenOff = route.highlightScreenOff,
                onBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<AboutRoute> {
            AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
