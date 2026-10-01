package com.xwab.app.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.back

/** A host can hide redundant back controls when the parent is visible alongside the detail. */
val LocalBackButtonVisibility = compositionLocalOf { true }

/**
 * A host showing several panes at once can make the arrow close the pane it sits in.
 *
 * Back removes the latest destination. With a category and its sound side by side, that is the
 * sound — so an arrow drawn on the category would close the other pane. Null keeps the caller's
 * own action, which is always the case while one destination fills the screen.
 */
val LocalBackButtonAction = compositionLocalOf<(() -> Unit)?> { null }

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalBackButtonVisibility.current) return
    IconButton(onClick = LocalBackButtonAction.current ?: onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.back),
            tint = SleepRelaxTheme.colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun BackButtonPreview() {
    SleepRelaxTheme { BackButton(onClick = {}) }
}
