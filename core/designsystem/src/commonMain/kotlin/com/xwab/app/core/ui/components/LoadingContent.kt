package com.xwab.app.core.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xwab.app.core.ui.theme.SleepRelaxTheme

/** Full-screen loading treatment shared by feature routes before their first content emission. */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    SleepRelaxBackground(modifier) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = SleepRelaxTheme.colors.accent,
        )
    }
}
