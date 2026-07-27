package com.xwab.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.xwab.app.core.ui.theme.SleepRelaxTheme

@Composable
fun SleepRelaxBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SleepRelaxTheme.colors.backgroundTop,
                        SleepRelaxTheme.colors.backgroundBottom,
                    ),
                ),
            ),
        content = content,
    )
}
