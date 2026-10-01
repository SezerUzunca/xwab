package com.xwab.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.pause
import xwab.designsystem.generated.resources.pause_content
import xwab.designsystem.generated.resources.play
import xwab.designsystem.generated.resources.play_content

/**
 * @param playRequested true while playback is requested, including loading or buffering, so users
 *   can cancel a pending start with the same pause action.
 * @param enabled false where there is nothing to play and nothing to pause. A list row always has
 *   both, so it is the screen for one sound that passes this.
 * @param contentTitle identifies the content controlled by this button to assistive technology.
 */
@Composable
// Keep optional size, enabled state and accessibility naming explicit at Compose call sites.
@Suppress("LongParameterList")
fun PlayPauseButton(
    playRequested: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    enabled: Boolean = true,
    contentTitle: String? = null,
) {
    val circleSize = if (large) {
        SleepRelaxTheme.dimens.largePlayCircleSize
    } else {
        SleepRelaxTheme.dimens.playIconCircleSize
    }
    val touchTargetSize = if (large) circleSize else SleepRelaxTheme.dimens.minimumTouchTarget

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(touchTargetSize),
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(SleepRelaxTheme.colors.primary.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playRequested) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (contentTitle == null) {
                    stringResource(if (playRequested) Res.string.pause else Res.string.play)
                } else {
                    stringResource(
                        if (playRequested) Res.string.pause_content else Res.string.play_content,
                        contentTitle,
                    )
                },
                tint = if (enabled) {
                    SleepRelaxTheme.colors.accent
                } else {
                    SleepRelaxTheme.colors.accent.copy(alpha = DISABLED_ALPHA)
                },
                modifier = Modifier.size(if (large) SleepRelaxTheme.dimens.iconLarge else SleepRelaxTheme.dimens.iconMedium),
            )
        }
    }
}

@Preview
@Composable
private fun PlayPauseButtonPreview() {
    SleepRelaxTheme {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayPauseButton(playRequested = false, onClick = {})
            PlayPauseButton(playRequested = true, onClick = {})
            PlayPauseButton(playRequested = false, onClick = {}, large = true)
            PlayPauseButton(playRequested = true, onClick = {}, enabled = false)
        }
    }
}
