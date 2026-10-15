package com.notch.neru.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TimerRoute

@Serializable
data class SettingsRoute(
    val highlightScreenOff: Boolean = false,
)

@Serializable
data object AboutRoute
