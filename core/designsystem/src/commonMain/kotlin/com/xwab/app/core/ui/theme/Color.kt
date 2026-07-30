package com.xwab.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SleepRelaxColors(
    val primary: Color,
    val accent: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val onSurface: Color,
    val glassWhite: Color,
    val glassWhiteOverlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color
)

val LocalSleepRelaxColors = staticCompositionLocalOf {
    SleepRelaxColors(
        primary = Color.Unspecified,
        accent = Color.Unspecified,
        backgroundTop = Color.Unspecified,
        backgroundBottom = Color.Unspecified,
        surface = Color.Unspecified,
        onSurface = Color.Unspecified,
        glassWhite = Color.Unspecified,
        glassWhiteOverlay = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        error = Color.Unspecified
    )
}

val darkColors = SleepRelaxColors(
    primary = Color(0xFFA78BFA),
    accent = Color(0xFFC4B5FD),
    backgroundTop = Color(0xFF0D1135),
    backgroundBottom = Color(0xFF1B0A2E),
    surface = Color(0xFF1A1040),
    onSurface = Color(0xFFE8E8F8),
    glassWhite = Color(0x14FFFFFF), // ~8% opaque white
    glassWhiteOverlay = Color(0x26FFFFFF), // ~15% opaque white
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.45f),
    error = Color(0xFFFFB4AB)
)
