package com.xwab.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SleepRelaxDimens(
    val spacingExtraSmall: Dp = 4.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 12.dp,
    val spacingLarge: Dp = 16.dp,
    val spacingExtraLarge: Dp = 24.dp,
    val spacingHuge: Dp = 32.dp,
    
    val paddingScreenHorizontal: Dp = 24.dp,
    val paddingScreenVertical: Dp = 48.dp,
    
    val iconSmall: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val minimumTouchTarget: Dp = 48.dp,
    
    val playIconCircleSize: Dp = 38.dp,
    val largePlayCircleSize: Dp = 68.dp,
    val albumArtSize: Dp = 220.dp,
    val albumArtInnerSize: Dp = 100.dp,

    val contentMaxWidth: Dp = 600.dp,
    val categoryCardMinWidth: Dp = 150.dp,
    val playerShortWindowMaxHeight: Dp = 900.dp,
)

val LocalSleepRelaxDimens = staticCompositionLocalOf { SleepRelaxDimens() }
