package com.xwab.app.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.result.ResultEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayableRow
import com.xwab.app.designsystem.components.FavoriteButton
import com.xwab.app.designsystem.components.SleepRelaxSnackbarHost
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.preparing
import xwab.designsystem.generated.resources.loop_duration
import xwab.designsystem.generated.resources.favorites_read_failed
import xwab.designsystem.generated.resources.favorite_write_failed
import xwab.feature.favorites.generated.resources.browse_sounds
import xwab.feature.favorites.generated.resources.favorite_removed
import xwab.feature.favorites.generated.resources.undo
import xwab.feature.favorites.generated.resources.retry_undo
import xwab.feature.favorites.generated.resources.Res
import xwab.feature.favorites.generated.resources.favorites_empty
import xwab.feature.favorites.generated.resources.favorites_title
import xwab.feature.favorites.generated.resources.sound_could_not_open
import xwab.feature.favorites.generated.resources.sound_not_found
import xwab.feature.favorites.generated.resources.sound_unavailable

@Composable
internal fun FavoritesScreenRoute(
    onTrackClick: (TrackId) -> Unit,
    onBrowse: () -> Unit,
    viewModel: FavoritesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    ResultEffect<FavoritesRoute> { scope.launch { listState.animateScrollToItem(0) } }
    val removedTrack by viewModel.removedTrack.collectAsStateWithLifecycle()
    val undoFailed by viewModel.undoFailed.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = removedTrack?.let { stringResource(Res.string.favorite_removed, it.name) }
    val undoLabel = stringResource(Res.string.undo)
    LaunchedEffect(removedTrack) {
        val track = removedTrack ?: return@LaunchedEffect
        // This restarts whenever the list comes back on screen; the ViewModel decides whether an
        // offer interrupted by leaving is still worth showing.
        if (!viewModel.claimUndoOffer(track.id)) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message.orEmpty(),
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoRemoval(track.id)
        else viewModel.acknowledgeRemoval(track.id)
    }
    Box(Modifier.fillMaxSize()) {
        when (val content = state) {
            FavoritesUiState.Loading -> LoadingContent()
            is FavoritesUiState.Ready -> FavoritesScreen(
                content.value, onTrackClick, viewModel::togglePlayback, viewModel::removeFavorite, onBrowse,
                onRetryUndo = if (undoFailed) {
                    { removedTrack?.let { viewModel.undoRemoval(it.id) } }
                } else null,
                listState = listState,
            )
        }
        SleepRelaxSnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// Independent screen events remain explicit so this stateless UI can be hosted and tested directly.
@Suppress("LongParameterList")
@Composable
internal fun FavoritesScreen(
    state: FavoritesState,
    onTrackClick: (TrackId) -> Unit,
    onPlaybackClick: (TrackId) -> Unit,
    onRemoveFavorite: (TrackId) -> Unit,
    onBrowse: () -> Unit,
    onRetryUndo: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    ScreenContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.favorites_title),
                    style = SleepRelaxTheme.typography.headlineLarge,
                    color = SleepRelaxTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = SleepRelaxTheme.dimens.spacingLarge).semantics { heading() },
                )
            }
            if (state.favoriteWriteFailed) {
                item { FavoriteWriteError(onRetryUndo) }
            }
            if (!state.favoritesAvailable) {
                item {
                    Text(
                        stringResource(UiRes.string.favorites_read_failed),
                        color = SleepRelaxTheme.colors.error,
                        style = SleepRelaxTheme.typography.bodyMedium,
                    )
                }
            }
            if (state.tracks.isEmpty() && state.favoritesAvailable) {
                item {
                    Column {
                        Text(
                            text = stringResource(Res.string.favorites_empty),
                            style = SleepRelaxTheme.typography.bodyLarge,
                            color = SleepRelaxTheme.colors.textSecondary,
                        )
                        TextButton(onClick = dropUnlessResumed(block = onBrowse)) {
                            Text(stringResource(Res.string.browse_sounds))
                        }
                    }
                }
            } else {
                items(state.tracks, key = { it.id.value }) { track ->
                    FavoriteRow(
                        track = track,
                        isPlaying = state.isRowPlaying(track.id),
                        isPreparing = state.isRowPreparing(track.id),
                        failure = state.rowFailure(track.id),
                        onTrackClick = onTrackClick,
                        onPlaybackClick = onPlaybackClick,
                        onRemoveFavorite = onRemoveFavorite,
                        favoritesAvailable = state.favoritesAvailable,
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteWriteError(onRetryUndo: (() -> Unit)?) {
    Column {
        Text(
            stringResource(UiRes.string.favorite_write_failed),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = SleepRelaxTheme.colors.error,
            style = SleepRelaxTheme.typography.bodyMedium,
        )
        if (onRetryUndo != null) {
            TextButton(onClick = onRetryUndo) { Text(stringResource(Res.string.retry_undo)) }
        }
    }
}

@Composable
private fun FavoriteRow(
    track: Track,
    isPlaying: Boolean,
    isPreparing: Boolean,
    failure: PlaybackFailure?,
    onTrackClick: (TrackId) -> Unit,
    onPlaybackClick: (TrackId) -> Unit,
    onRemoveFavorite: (TrackId) -> Unit,
    favoritesAvailable: Boolean,
) {
    // Every question about this row is the state's to answer; this only draws what comes back.
    PlayableRow(
        title = track.name,
        subtitle = stringResource(UiRes.string.loop_duration, formatDuration(track.durationSeconds)),
        playRequested = isPlaying,
        onClick = dropUnlessResumed { onTrackClick(track.id) },
        onPlayPauseClick = { onPlaybackClick(track.id) },
        statusMessage = stringResource(UiRes.string.preparing)
            .takeIf { isPreparing },
        errorMessage = failure?.let { stringResource(it.messageResource()) },
        trailingContent = {
            FavoriteButton(isFavorite = true, enabled = favoritesAvailable, contentTitle = track.name,
                onClick = { onRemoveFavorite(track.id) })
        },
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
            onRemoveFavorite = {},
            onBrowse = {},
        )
    }
}
