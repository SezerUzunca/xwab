package com.xwab.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.core.designsystem.generated.resources.Res
import xwab.core.designsystem.generated.resources.pause
import xwab.core.designsystem.generated.resources.play

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
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
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .background(SleepRelaxTheme.colors.primary.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                tint = SleepRelaxTheme.colors.accent,
                modifier = Modifier.size(if (large) SleepRelaxTheme.dimens.iconLarge else SleepRelaxTheme.dimens.iconMedium),
            )
        }
    }
}
