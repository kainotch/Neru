package com.notch.neru.feature.timer.theme

import androidx.compose.ui.graphics.Color
import com.notch.neru.core.data.model.ThemeId
import com.notch.neru.feature.timer.R

object AppThemes {

    val ScreenOffDark: AppTheme = AppTheme(
        id = ThemeId.Midnight,
        labelRes = R.string.theme_midnight,
        hasGradient = false,
        bgTop = Color(0xFF0C0E12),
        bgMid = Color(0xFF0C0E12),
        bgBot = Color(0xFF0C0E12),
        bgSolid = Color(0xFF0C0E12),
        auroraColor = Color.Transparent,
        starColor = Color.Transparent,
        allowStars = false,
        accent = Color(0xFFFFFFFF),
        accentInk = Color(0xFF0C0E12),
        surface1 = Color(0xFF15181E),
        surface2 = Color(0xFF222630),
        stroke = Color.Transparent,
        strokeStrong = Color.Transparent,
        textPrimary = Color(0xFFFFFFFF),
        textDim = Color(0xFFA2ABB6),
        textMuted = Color(0xFF6E7886),
        textFaint = Color(0xFF3F4652),
        dialPlateTop = Color.Transparent,
        dialPlateBot = Color.Transparent,
        dialWell = Color(0xFF0C0E12),
        dialTrack = Color(0xFF1A1D24),
        dialTickMajor = Color.Transparent,
        dialTickMinor = Color.Transparent,
        knobBody = Color(0xFFFFFFFF),
        hourDotInactive = Color.Transparent,
        isDark = true,
    )

    val Midnight: AppTheme = ScreenOffDark
    val Ocean: AppTheme = ScreenOffDark
    val Ember: AppTheme = ScreenOffDark
    val Light: AppTheme = ScreenOffDark
    val Basic: AppTheme = ScreenOffDark
    val Amoled: AppTheme = ScreenOffDark

    val All: List<AppTheme> = listOf(ScreenOffDark)

    fun byId(id: ThemeId): AppTheme = ScreenOffDark
}

/** Factory for dark-on-gradient themes that only differ in their 3 gradient stops + accent. */
private fun darkThemeOf(
    id: ThemeId,
    labelRes: Int,
    bgTop: Color,
    bgMid: Color,
    bgBot: Color,
    accent: Color,
): AppTheme = AppTheme(
    id = id,
    labelRes = labelRes,
    hasGradient = true,
    bgTop = bgTop,
    bgMid = bgMid,
    bgBot = bgBot,
    bgSolid = bgMid,
    auroraColor = accent.copy(alpha = 0.25f),
    starColor = Color(0xFFFFFFFF),
    allowStars = true,
    accent = accent,
    accentInk = bgMid,
    surface1 = Color(0x0AFFFFFF),
    surface2 = Color(0x10FFFFFF),
    stroke = Color.Transparent,
    strokeStrong = Color.Transparent,
    textPrimary = Color(0xFFFFFFFF),
    textDim = Color(0x8CFFFFFF),
    textMuted = Color(0x73FFFFFF),
    textFaint = Color(0x4DFFFFFF),
    dialPlateTop = Color(0x0FFFFFFF),
    dialPlateBot = Color(0x04FFFFFF),
    dialWell = Color(0x40000000),
    dialTrack = Color(0x0FFFFFFF),
    dialTickMajor = Color(0x59FFFFFF),
    dialTickMinor = Color(0x1FFFFFFF),
    knobBody = Color(0xFFFFFFFF),
    hourDotInactive = Color(0x2EFFFFFF),
    isDark = true,
)
