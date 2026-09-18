package com.xwab.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/**
 * The app gradient, and nothing else.
 *
 * Internal because a screen wants [ScreenContainer], which is this plus the width its content is
 * held to — the two were always written together, and the pair is the thing with a meaning. This
 * survives as a piece of its own only because [LoadingContent] needs the gradient without the
 * frame.
 */
@Composable
internal fun SleepRelaxBackground(
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
