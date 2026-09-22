package com.xwab.app.feature.nowplaying

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.preparing
import xwab.feature.nowplaying.generated.resources.Res
import xwab.feature.nowplaying.generated.resources.item_could_not_open
import xwab.feature.nowplaying.generated.resources.item_not_found
import xwab.feature.nowplaying.generated.resources.item_unavailable
import xwab.feature.nowplaying.generated.resources.now_playing
import xwab.feature.nowplaying.generated.resources.open_now_playing

/**
 * What the session is on, wherever the listener is.
 *
 * The visibility rule lives here rather than in the shell that places it: whether there is anything
 * to show is this feature's own question, and a shell that answered it would need to know what an
 * idle session looks like.
 */
@Composable
internal fun NowPlayingScreen(
    state: NowPlayingState,
    onPlayPauseClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isIdle) return

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = SleepRelaxTheme.colors.glassWhiteOverlay)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleepRelaxTheme.colors.backgroundBottom)
                // The whole bar except the transport control, which has its own. A listener who
                // started something from a row and then walked away from that screen has no other
                // way back to it.
                .clickable(onClickLabel = stringResource(Res.string.open_now_playing)) {
                    onOpenClick()
                }
                .padding(
                    start = SleepRelaxTheme.dimens.spacingLarge,
                    end = SleepRelaxTheme.dimens.spacingSmall,
                    top = SleepRelaxTheme.dimens.spacingSmall,
                    bottom = SleepRelaxTheme.dimens.spacingSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // The session withholds a title for exactly as long as it is switching, so this
                    // fallback is what a bar reads during a switch rather than the outgoing name.
                    text = state.title ?: stringResource(Res.string.now_playing),
                    style = SleepRelaxTheme.typography.titleSmall,
                    color = SleepRelaxTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // One second line, and a failure outranks a status: an item that will not play is
                // not still loading, and saying both would be saying one of them wrongly.
                val failure = state.failure
                when {
                    failure != null -> Text(
                        text = stringResource(
                            when (failure) {
                                is PlaybackFailure.ItemNotFound -> Res.string.item_not_found
                                is PlaybackFailure.SourceUnavailable -> Res.string.item_unavailable
                                is PlaybackFailure.EngineFailed -> Res.string.item_could_not_open
                            },
                        ),
                        // The style every failure in this app is written in. This was the only one
                        // in `labelMedium`, a size below the rest, which made the one message
                        // with no screen behind it to repeat it the quietest of them all.
                        style = SleepRelaxTheme.typography.bodyMedium,
                        color = SleepRelaxTheme.colors.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The same word every list in this app uses for the same state, taken from the
                    // design system rather than spelled again here.
                    state.isPreparing -> Text(
                        text = stringResource(UiRes.string.preparing),
                        style = SleepRelaxTheme.typography.labelMedium,
                        color = SleepRelaxTheme.colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            PlayPauseButton(isPlaying = state.playIntent, onClick = onPlayPauseClick)
        }
    }
}

@Preview
@Composable
private fun NowPlayingScreenPreview() {
    SleepRelaxTheme {
        NowPlayingScreen(
            state = NowPlayingState(
                itemId = PlaybackItemId(ANY_KIND, "calm-waves"),
                title = "Calm Waves",
                playIntent = true,
            ),
            onPlayPauseClick = {},
            onOpenClick = {},
        )
    }
}

/**
 * This feature draws whatever is playing and never asks what kind it is, so its own fixtures
 * name a kind that belongs to no content module.
 */
private const val ANY_KIND = "any-kind"
