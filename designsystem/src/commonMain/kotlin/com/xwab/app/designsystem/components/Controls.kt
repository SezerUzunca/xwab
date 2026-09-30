package com.xwab.app.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 * A choice among several, drawn as a pill so it reads as a button rather than a label.
 * Narrower padding than Material's default, so a short row of them fits one line on a phone.
 */
@Composable
fun SleepRelaxOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val border = SleepRelaxTheme.colors.accent.copy(alpha = if (enabled) BORDER_ALPHA else DISABLED_BORDER_ALPHA)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = SleepRelaxTheme.shapes.full,
        border = BorderStroke(1.dp, border),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleepRelaxTheme.colors.accent),
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

private const val BORDER_ALPHA = 0.6f
private const val DISABLED_BORDER_ALPHA = 0.2f
