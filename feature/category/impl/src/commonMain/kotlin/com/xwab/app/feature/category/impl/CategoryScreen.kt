package com.xwab.app.feature.category.impl

import com.xwab.app.core.catalog.TrackId
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.Category
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xwab.feature.category.`impl`.generated.resources.*
import xwab.core.designsystem.generated.resources.Res as UiRes
import xwab.core.designsystem.generated.resources.duration_public_domain

import com.xwab.app.core.ui.format.formatDuration
import com.xwab.app.core.ui.components.BackButton
import com.xwab.app.core.ui.components.FavoriteButton
import com.xwab.app.core.ui.components.MusicCard
import com.xwab.app.core.ui.components.LoadingContent
import com.xwab.app.core.ui.components.SleepRelaxBackground
import com.xwab.app.core.ui.components.PlayPauseButton
import com.xwab.app.core.ui.state.Loadable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn

@Composable
internal fun CategoryScreenRoute(
    onMusicClick: (musicId: TrackId) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val content = state) {
        Loadable.Loading -> LoadingContent()
        is Loadable.Ready -> CategoryScreen(
            state = content.value,
            onMusicClick = onMusicClick,
            onFavoriteClick = viewModel::toggleFavorite,
            onPlaybackClick = viewModel::togglePlayback,
            onBack = onBack,
        )
    }
}

@Composable
internal fun CategoryScreen(
    state: CategoryState,
    onMusicClick: (musicId: TrackId) -> Unit,
    onFavoriteClick: (musicId: TrackId) -> Unit,
    onPlaybackClick: (musicId: TrackId) -> Unit,
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
                    state.musics.size,
                    state.musics.size,
                ),
                style = SleepRelaxTheme.typography.bodyMedium,
                color = SleepRelaxTheme.colors.textSecondary,
            )

            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingHuge))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall)) {
                items(state.musics, key = { it.id.value }) { music ->
                    MusicCard(
                        title = music.name,
                        subtitle = stringResource(UiRes.string.duration_public_domain, formatDuration(music.durationSeconds)),
                        onClick = { onMusicClick(music.id) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlayPauseButton(
                                    isPlaying = state.requestedTrackId == music.id && state.playIntent,
                                    onClick = { onPlaybackClick(music.id) },
                                )
                                FavoriteButton(
                                    isFavorite = music.id in state.favoriteIds,
                                    onClick = { onFavoriteClick(music.id) },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CategoryScreenPreview() {
    SleepRelaxTheme {
        CategoryScreen(
            state = CategoryState(
                category = Category(CategoryId("rain"), "Rain", "Gentle raindrops", "☂", 1),
                musics = listOf(
                    Music(
                        id = TrackId("gentle-rain"),
                        name = "Rain on the Window",
                        categoryId = CategoryId("rain"),
                        durationSeconds = 9,
                    ),
                ),
                favoriteIds = setOf(TrackId("gentle-rain")),
            ),
            onMusicClick = {},
            onFavoriteClick = {},
            onPlaybackClick = {},
            onBack = {},
        )
    }
}
