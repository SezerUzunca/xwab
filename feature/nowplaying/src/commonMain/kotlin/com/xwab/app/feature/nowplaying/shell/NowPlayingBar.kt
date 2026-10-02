package com.xwab.app.feature.nowplaying.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.SleepRelaxTextButton
import com.xwab.app.designsystem.format.remainingWholeMinutes
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.nowplaying.NowPlayingState
import com.xwab.app.feature.nowplaying.NowPlayingUiState
import com.xwab.app.feature.nowplaying.NowPlayingViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.cancel_timer
import xwab.designsystem.generated.resources.sleep_timer
import xwab.designsystem.generated.resources.timer_duration_minutes
import xwab.feature.nowplaying.generated.resources.Res
import xwab.feature.nowplaying.generated.resources.item_could_not_open
import xwab.feature.nowplaying.generated.resources.item_not_found
import xwab.feature.nowplaying.generated.resources.item_unavailable
import xwab.feature.nowplaying.generated.resources.now_playing
import xwab.feature.nowplaying.generated.resources.open_details
import xwab.feature.nowplaying.generated.resources.player_loading
import xwab.feature.nowplaying.generated.resources.player_paused
import xwab.feature.nowplaying.generated.resources.player_playing
import xwab.feature.nowplaying.generated.resources.player_stops_in

/**
 * The strip under every tab: what is playing, its play/pause, and a way to its own screen.
 *
 * Shown while something is requested, and while a timer runs with nothing requested: that timer
 * still stops whatever starts next, and with no item there is no screen to cancel it from, so the
 * bar offers the cancel itself.
 *
 * @param onOpen opens the item's own screen; which screen that is, is the app's decision.
 * @param hiddenFor the item whose own screen is showing. That screen already has this item's
 *   play/pause and timer, so a bar for the same item would be a second copy of both. Null, or any
 *   other item, keeps the bar.
 */
@Composable
fun NowPlayingBar(
    onOpen: (PlaybackItemId) -> Unit,
    modifier: Modifier = Modifier,
    hiddenFor: PlaybackItemId? = null,
) {
    val viewModel: NowPlayingViewModel = metroViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    // A bar has no loading look of its own: until the session answers there is nothing to draw.
    val content = (state as? NowPlayingUiState.Ready)?.value ?: return
    if (!content.showsBarBeside(hiddenFor, sleepTimerRemainingMs)) return
    NowPlayingBarContent(
        state = content,
        onPlayPauseClick = viewModel::togglePlayback,
        onOpenClick = dropUnlessResumed { content.itemId?.let(onOpen) },
        onTimerCancel = viewModel::cancelSleepTimer,
        modifier = modifier,
        sleepTimerRemainingMs = sleepTimerRemainingMs,
    )
}

/** What the bar draws, from values alone; the part screen tests render without a ViewModel. */
@Composable
@Suppress("LongParameterList") // The bar's three actions plus the timer it reports.
internal fun NowPlayingBarContent(
    state: NowPlayingState,
    onPlayPauseClick: () -> Unit,
    onOpenClick: () -> Unit,
    onTimerCancel: () -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerRemainingMs: Long? = null,
) {
    val title = when {
        !state.isIdle -> state.title ?: stringResource(Res.string.now_playing)
        else -> stringResource(UiRes.string.sleep_timer)
    }
    // With nothing requested there is no item screen to open, so only an item makes the bar a link.
    val openable = if (state.isIdle) {
        Modifier
    } else {
        Modifier.clickable(onClickLabel = stringResource(Res.string.open_details), onClick = onOpenClick)
    }
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = SleepRelaxTheme.colors.glassWhiteOverlay)
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(SleepRelaxTheme.colors.backgroundBottom)
                .then(openable)
                .padding(
                    horizontal = SleepRelaxTheme.dimens.spacingLarge,
                    vertical = SleepRelaxTheme.dimens.spacingSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = SleepRelaxTheme.typography.titleSmall,
                    color = SleepRelaxTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // One status line. Loading says what is happening now, so it goes before the timer;
                // the tap target's own label is never shown as text. The timer reads in whole
                // minutes, exactly as the timer card on an item's screen shows it.
                val timerLeft = sleepTimerRemainingMs?.let {
                    stringResource(UiRes.string.timer_duration_minutes, remainingWholeMinutes(it))
                }
                Text(
                    text = when {
                        state.isPreparing -> stringResource(Res.string.player_loading)
                        timerLeft != null -> stringResource(Res.string.player_stops_in, timerLeft)
                        state.playIntent -> stringResource(Res.string.player_playing)
                        else -> stringResource(Res.string.player_paused)
                    },
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
                state.failure?.let { PlaybackFailureText(it) }
            }
            if (state.isIdle) {
                SleepRelaxTextButton(onClick = onTimerCancel) { Text(stringResource(UiRes.string.cancel_timer)) }
            } else {
                PlayPauseButton(playRequested = state.playIntent, onClick = onPlayPauseClick, contentTitle = title)
            }
        }
    }
}

/** The current item's failure, announced once, under the title it refers to. */
@Composable
private fun PlaybackFailureText(failure: PlaybackFailure) {
    Text(
        text = stringResource(
            when (failure) {
                is PlaybackFailure.ItemNotFound -> Res.string.item_not_found
                is PlaybackFailure.SourceUnavailable -> Res.string.item_unavailable
                is PlaybackFailure.EngineFailed -> Res.string.item_could_not_open
            },
        ),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = SleepRelaxTheme.typography.bodyMedium,
        color = SleepRelaxTheme.colors.error,
    )
}

private const val PREVIEW_TIMER_MS = 899_000L

@Preview
@Composable
private fun NowPlayingBarPreview() {
    SleepRelaxTheme {
        NowPlayingBarContent(
            state = NowPlayingState(
                itemId = PlaybackItemId("sound", "calm-waves"),
                title = "Ontario Waves",
                playIntent = true,
            ),
            onPlayPauseClick = {},
            onOpenClick = {},
            onTimerCancel = {},
            sleepTimerRemainingMs = PREVIEW_TIMER_MS,
        )
    }
}

/** Nothing requested and a timer still running: the bar is not a link and offers the cancel. */
@Preview
@Composable
private fun NowPlayingBarTimerOnlyPreview() {
    SleepRelaxTheme {
        NowPlayingBarContent(
            state = NowPlayingState(),
            onPlayPauseClick = {},
            onOpenClick = {},
            onTimerCancel = {},
            sleepTimerRemainingMs = PREVIEW_TIMER_MS,
        )
    }
}
