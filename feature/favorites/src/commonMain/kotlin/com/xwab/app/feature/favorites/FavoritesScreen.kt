package com.xwab.app.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayableRow
import com.xwab.app.designsystem.components.SleepRelaxBackground
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.preparing
import xwab.designsystem.generated.resources.favorites_read_failed
import xwab.feature.favorites.generated.resources.Res
import xwab.feature.favorites.generated.resources.favorites_empty
import xwab.feature.favorites.generated.resources.favorites_title
import xwab.feature.favorites.generated.resources.sound_could_not_open
import xwab.feature.favorites.generated.resources.sound_not_found
import xwab.feature.favorites.generated.resources.sound_unavailable

@Composable
internal fun FavoritesScreenRoute(
    onTrackClick: (TrackId) -> Unit,
    viewModel: FavoritesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> FavoritesScreen(content.value, onTrackClick, viewModel::togglePlayback)
    }
}

@Composable
internal fun FavoritesScreen(
    state: FavoritesState,
    onTrackClick: (TrackId) -> Unit,
    onPlaybackClick: (TrackId) -> Unit,
) {
    SleepRelaxBackground {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                vertical = SleepRelaxTheme.dimens.paddingScreenVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.favorites_title),
                    style = SleepRelaxTheme.typography.headlineLarge,
                    color = SleepRelaxTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = SleepRelaxTheme.dimens.spacingLarge),
                )
            }
            if (!state.favoritesAvailable) {
                item {
                    Text(stringResource(UiRes.string.favorites_read_failed), color = SleepRelaxTheme.colors.error)
                }
            }
            if (state.tracks.isEmpty() && state.favoritesAvailable) {
                item {
                    Text(
                        text = stringResource(Res.string.favorites_empty),
                        style = SleepRelaxTheme.typography.bodyLarge,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                }
            } else {
                items(state.tracks, key = { it.id.value }) { track ->
                    FavoriteRow(track, state, onTrackClick, onPlaybackClick)
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    track: Track,
    state: FavoritesState,
    onTrackClick: (TrackId) -> Unit,
    onPlaybackClick: (TrackId) -> Unit,
) {
    // Every question about this row is the state's to answer; this only draws what comes back.
    PlayableRow(
        title = track.name,
        subtitle = formatDuration(track.durationSeconds),
        isPlaying = state.isRowPlaying(track.id),
        onClick = { onTrackClick(track.id) },
        onPlayPauseClick = { onPlaybackClick(track.id) },
        statusMessage = stringResource(UiRes.string.preparing)
            .takeIf { state.isRowPreparing(track.id) },
        errorMessage = state.rowFailure(track.id)?.let { stringResource(it.messageResource()) },
    )
}

/** Sound wording, because this list only ever holds sounds. */
private fun PlaybackFailure.messageResource() = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.sound_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.sound_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.sound_could_not_open
}

@Preview
@Composable
private fun FavoritesScreenPreview() {
    SleepRelaxTheme {
        FavoritesScreen(
            state = FavoritesState(
                tracks = listOf(
                    Track(TrackId("rain"), "Rain", CategoryId("weather"), durationSeconds = 60),
                ),
            ),
            onTrackClick = {},
            onPlaybackClick = {},
        )
    }
}
