package com.xwab.app.feature.favorites.impl

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
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackFailure
import com.xwab.app.core.ui.components.LoadingContent
import com.xwab.app.core.ui.components.PlayableRow
import com.xwab.app.core.ui.components.SleepRelaxBackground
import com.xwab.app.core.ui.format.formatDuration
import com.xwab.app.core.ui.state.Loadable
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.core.designsystem.generated.resources.Res as UiRes
import xwab.core.designsystem.generated.resources.preparing
import xwab.feature.favorites.`impl`.generated.resources.Res
import xwab.feature.favorites.`impl`.generated.resources.favorites_empty
import xwab.feature.favorites.`impl`.generated.resources.favorites_title
import xwab.feature.favorites.`impl`.generated.resources.sound_could_not_open
import xwab.feature.favorites.`impl`.generated.resources.sound_not_found
import xwab.feature.favorites.`impl`.generated.resources.sound_unavailable

@Composable
fun FavoritesScreenRoute(component: FavoritesComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> FavoritesScreen(content.value, component.onMusicClick, component::togglePlayback)
    }
}

@Composable
internal fun FavoritesScreen(
    state: FavoritesState,
    onMusicClick: (TrackId) -> Unit,
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
            if (state.musics.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.favorites_empty),
                        style = SleepRelaxTheme.typography.bodyLarge,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                }
            } else {
                items(state.musics, key = { it.id.value }) { music ->
                    FavoriteRow(music, state, onMusicClick, onPlaybackClick)
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    music: Music,
    state: FavoritesState,
    onMusicClick: (TrackId) -> Unit,
    onPlaybackClick: (TrackId) -> Unit,
) {
    val isRequested = state.requestedTrackId == music.id
    // The failure names its own item, which is not [FavoritesState.requestedTrackId] by the time it
    // arrives: a failed lookup has already released the session's claim.
    val failure = state.playbackFailure?.takeIf { it.itemId.value == music.id.value }

    PlayableRow(
        title = music.name,
        subtitle = formatDuration(music.durationSeconds),
        isPlaying = isRequested && state.playIntent,
        onClick = { onMusicClick(music.id) },
        onPlayPauseClick = { onPlaybackClick(music.id) },
        statusMessage = stringResource(UiRes.string.preparing)
            .takeIf { isRequested && state.isPreparing },
        errorMessage = failure?.let { stringResource(it.messageResource()) },
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
                musics = listOf(
                    Music(TrackId("rain"), "Rain", CategoryId("weather"), durationSeconds = 60),
                ),
            ),
            onMusicClick = {},
            onPlaybackClick = {},
        )
    }
}
