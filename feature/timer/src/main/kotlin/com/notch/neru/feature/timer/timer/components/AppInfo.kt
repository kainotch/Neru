package com.notch.neru.feature.timer.timer.components

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

data class TargetAppItem(
    val packageName: String,
    val name: String,
    val accentColor: Color,
    val icon: Drawable? = null,
    val isCustom: Boolean = false,
)

object TargetAppDefaults {
    val DEFAULT_APPS = listOf(
        TargetAppItem("com.spotify.music", "Spotify", Color(0xFF1DB954)),
        TargetAppItem("com.instagram.android", "Instagram", Color(0xFFE1306C)),
        TargetAppItem("com.google.android.youtube", "YouTube", Color(0xFFFF0000)),
        TargetAppItem("com.facebook.katana", "Facebook", Color(0xFF1877F2)),
        TargetAppItem("com.netflix.mediaclient", "Netflix", Color(0xFFE50914)),
        TargetAppItem("com.google.android.apps.youtube.music", "YT Music", Color(0xFFFF4E4E)),
    )
}
