package com.xwab.app.designsystem.theme

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
    // 0.60, not the 0.45 this started at. Over the backgrounds this app actually draws, 0.45 came
    // out between 4.2:1 and 4.4:1 — under the 4.5:1 WCAG AA asks for text this size, and this is
    // the colour every duration, status line, sleep-timer countdown and unselected tab label is
    // drawn in. The tightest case was a [glassWhite] card, which lightens the gradient beneath it.
    // ContrastTest measures all of them, so the next adjustment cannot quietly drop back under.
    textSecondary = Color.White.copy(alpha = 0.60f),
    error = Color(0xFFFFB4AB)
)
