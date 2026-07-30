package com.xwab.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class SleepRelaxTypography(
    val headlineLarge: TextStyle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Thin,
        letterSpacing = 6.sp
    ),
    val headlineMedium: TextStyle = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Thin,
        letterSpacing = 4.sp
    ),
    val headlineSmall: TextStyle = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Thin,
        letterSpacing = 2.sp
    ),
    val titleLarge: TextStyle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Normal
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Light
    ),
    val titleSmall: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 1.sp
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    val bodySmall: TextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.sp
    ),
    val labelMedium: TextStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.sp
    )
)

val LocalSleepRelaxTypography = staticCompositionLocalOf { SleepRelaxTypography() }
