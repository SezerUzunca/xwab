package com.xwab.app.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.dp

@Immutable
data class SleepRelaxShapes(
    val small: CornerBasedShape = RoundedCornerShape(8.dp),
    val medium: CornerBasedShape = RoundedCornerShape(16.dp),
    val large: CornerBasedShape = RoundedCornerShape(20.dp),
    val full: CornerBasedShape = RoundedCornerShape(100)
)

val LocalSleepRelaxShapes = staticCompositionLocalOf { SleepRelaxShapes() }
