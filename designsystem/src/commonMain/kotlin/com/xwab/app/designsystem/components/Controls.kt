package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/** Shared control styling; callers own values, labels and actions. */
@Composable
fun SleepRelaxTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = SleepRelaxTheme.colors.accent),
        content = content,
    )
}

/**
 * Snackbars in the app's night palette.
 *
 * Material draws a snackbar on its inverse surface, which in a dark scheme is near-white — a bright
 * panel in a room the listener is trying to keep dark. This keeps it on the app's own surface.
 * No outline: the data overload pads the snackbar inside whatever modifier it is given, so a border
 * here would frame the padding rather than the panel.
 */
@Composable
fun SleepRelaxSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = SleepRelaxTheme.colors.surface,
            contentColor = SleepRelaxTheme.colors.textPrimary,
            actionColor = SleepRelaxTheme.colors.accent,
            dismissActionContentColor = SleepRelaxTheme.colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun SleepRelaxTextButtonPreview() {
    SleepRelaxTheme {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SleepRelaxTextButton(onClick = {}) { Text("Cancel") }
            SleepRelaxTextButton(onClick = {}, enabled = false) { Text("Disabled") }
        }
    }
}

@Preview
@Composable
private fun SleepRelaxSnackbarHostPreview() {
    SleepRelaxTheme {
        val hostState = remember { SnackbarHostState() }
        LaunchedEffect(hostState) { hostState.showSnackbar("Audio unavailable") }
        SleepRelaxSnackbarHost(hostState)
    }
}
