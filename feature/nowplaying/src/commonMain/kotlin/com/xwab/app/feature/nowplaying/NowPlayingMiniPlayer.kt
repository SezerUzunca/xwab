package com.xwab.app.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.format.formatRemaining
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.sleep_timer
import xwab.feature.nowplaying.generated.resources.Res
import xwab.feature.nowplaying.generated.resources.item_could_not_open
import xwab.feature.nowplaying.generated.resources.item_not_found
import xwab.feature.nowplaying.generated.resources.item_unavailable
import xwab.feature.nowplaying.generated.resources.now_playing
import xwab.feature.nowplaying.generated.resources.open_now_playing
import xwab.feature.nowplaying.generated.resources.player_loading
import xwab.feature.nowplaying.generated.resources.player_paused
import xwab.feature.nowplaying.generated.resources.player_playing
import xwab.feature.nowplaying.generated.resources.player_stops_in

@Composable
internal fun NowPlayingMiniPlayerRoute(
    onOpen: () -> Unit,
    viewModel: NowPlayingViewModel,
    modifier: Modifier = Modifier,
    hiddenFor: PlaybackItemId? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.showsMiniPlayerBeside(hiddenFor)) return
    NowPlayingMiniPlayer(state, viewModel::togglePlayback, dropUnlessResumed { onOpen() }, modifier)
}

/**
 * Drawn while something is requested, and while a timer runs with nothing requested: that timer
 * still stops whatever starts next, so it needs a way back to the player that can cancel it.
 */
@Composable
internal fun NowPlayingMiniPlayer(
    state: NowPlayingState,
    onPlayPauseClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.showsMiniPlayer) return
    val title = when {
        !state.isIdle -> state.title ?: stringResource(Res.string.now_playing)
        else -> stringResource(UiRes.string.sleep_timer)
    }
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = SleepRelaxTheme.colors.glassWhiteOverlay)
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(SleepRelaxTheme.colors.backgroundBottom)
                .clickable(onClickLabel = stringResource(Res.string.open_now_playing), onClick = onOpenClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = SleepRelaxTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text(title, style = SleepRelaxTheme.typography.titleSmall,
                    color = SleepRelaxTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // One status line. Loading says what is happening now, so it goes before the timer;
                // the tap target's own label is never shown as text.
                val remainingMs = state.sleepTimerRemainingMs
                Text(
                    text = when {
                        state.isPreparing -> stringResource(Res.string.player_loading)
                        remainingMs != null -> stringResource(Res.string.player_stops_in, formatRemaining(remainingMs))
                        state.playIntent -> stringResource(Res.string.player_playing)
                        else -> stringResource(Res.string.player_paused)
                    },
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
                PlaybackStatus(state, showLoading = false)
            }
            if (!state.isIdle) {
                PlayPauseButton(playRequested = state.playIntent, onClick = onPlayPauseClick, contentTitle = title)
            }
        }
    }
}

/** A failure for this item, then loading. [showLoading] is false where loading already has a line. */
@Composable
internal fun PlaybackStatus(state: NowPlayingState, showLoading: Boolean = true) {
    val failure = state.failure
    if (failure != null) {
        Text(
            stringResource(when (failure) {
                is PlaybackFailure.ItemNotFound -> Res.string.item_not_found
                is PlaybackFailure.SourceUnavailable -> Res.string.item_unavailable
                is PlaybackFailure.EngineFailed -> Res.string.item_could_not_open
            }),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.error,
        )
    } else if (showLoading && state.isPreparing) {
        Text(stringResource(Res.string.player_loading), style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary)
    }
}
