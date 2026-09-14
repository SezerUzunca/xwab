package com.xwab.app.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.FavoriteButton
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayableRow
import com.xwab.app.designsystem.components.SleepRelaxBackground
import com.xwab.app.designsystem.format.formatDuration
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res as UiRes
import xwab.designsystem.generated.resources.duration_public_domain
import xwab.designsystem.generated.resources.preparing
import xwab.feature.category.generated.resources.Res
import xwab.feature.category.generated.resources.category_track_count
import xwab.feature.category.generated.resources.sound_could_not_open
import xwab.feature.category.generated.resources.sound_not_found
import xwab.feature.category.generated.resources.sound_unavailable

@Composable
internal fun CategoryScreenRoute(
    onTrackClick: (trackId: TrackId) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> CategoryScreen(
            state = content.value,
            onTrackClick = onTrackClick,
            onFavoriteClick = viewModel::toggleFavorite,
            onPlaybackClick = viewModel::togglePlayback,
            onBack = onBack,
        )
    }
}

@Composable
internal fun CategoryScreen(
    state: CategoryState,
    onTrackClick: (trackId: TrackId) -> Unit,
    onFavoriteClick: (trackId: TrackId) -> Unit,
    onPlaybackClick: (trackId: TrackId) -> Unit,
    onBack: () -> Unit,
) {
    SleepRelaxBackground {
        Column(
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(
                    horizontal = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                    vertical = SleepRelaxTheme.dimens.paddingScreenVertical
                ),
        ) {
            BackButton(onClick = onBack)

            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Text(
                text = state.category?.name.orEmpty(),
                style = SleepRelaxTheme.typography.headlineMedium,
                color = SleepRelaxTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
            Text(
                text = pluralStringResource(
                    Res.plurals.category_track_count,
                    state.tracks.size,
                    state.tracks.size,
                ),
                style = SleepRelaxTheme.typography.bodyMedium,
                color = SleepRelaxTheme.colors.textSecondary,
            )

            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingHuge))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall)) {
                items(state.tracks, key = { it.id.value }) { track ->
                    // Every question about this row is the state's to answer; this only draws what
                    // comes back. The same row the favorites and story lists draw, so a tap that
                    // cannot be served says so here too.
                    PlayableRow(
                        title = track.name,
                        subtitle = stringResource(
                            UiRes.string.duration_public_domain,
                            formatDuration(track.durationSeconds),
                        ),
                        isPlaying = state.isRowPlaying(track.id),
                        onClick = { onTrackClick(track.id) },
                        onPlayPauseClick = { onPlaybackClick(track.id) },
                        statusMessage = stringResource(UiRes.string.preparing)
                            .takeIf { state.isRowPreparing(track.id) },
                        errorMessage = state.rowFailure(track.id)
                            ?.let { stringResource(it.messageResource()) },
                        trailingContent = {
                            FavoriteButton(
                                isFavorite = state.isRowFavorite(track.id),
                                onClick = { onFavoriteClick(track.id) },
                            )
                        },
                    )
                }
            }
        }
    }
}

/** Sound wording, because this list only ever holds sounds. */
private fun PlaybackFailure.messageResource() = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.sound_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.sound_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.sound_could_not_open
}

@Preview
@Composable
private fun CategoryScreenPreview() {
    SleepRelaxTheme {
        CategoryScreen(
            state = CategoryState(
                category = Category(CategoryId("rain"), "Rain", "Gentle raindrops", "☂", 1),
                tracks = listOf(
                    Track(
                        id = TrackId("gentle-rain"),
                        name = "Rain on the Window",
                        categoryId = CategoryId("rain"),
                        durationSeconds = 9,
                    ),
                ),
                favoriteIds = setOf(TrackId("gentle-rain")),
            ),
            onTrackClick = {},
            onFavoriteClick = {},
            onPlaybackClick = {},
            onBack = {},
        )
    }
}
