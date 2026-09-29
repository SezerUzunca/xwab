package com.xwab.app.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class SleepRelaxTypography(
    val headlineLarge: TextStyle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    val headlineSmall: TextStyle = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    val titleLarge: TextStyle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 34.sp
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
    ),
    val titleSmall: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    val bodySmall: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    val labelMedium: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
)

val LocalSleepRelaxTypography = staticCompositionLocalOf { SleepRelaxTypography() }
