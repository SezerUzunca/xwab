package com.xwab.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import com.xwab.app.core.ui.theme.SleepRelaxTheme

fun Modifier.glassCard(): Modifier = composed {
    this
        .clip(SleepRelaxTheme.shapes.medium)
        .background(SleepRelaxTheme.colors.glassWhite)
}
