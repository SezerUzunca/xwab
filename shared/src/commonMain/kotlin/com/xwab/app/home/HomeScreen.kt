package com.xwab.app.home

import com.xwab.app.core.catalog.TrackId
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.ui.theme.SleepRelaxTheme
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import xwab.shared.generated.resources.*
// The app's name and tagline belong to the app, not to whichever slice happens to draw them.
import xwab.core.designsystem.generated.resources.Res as UiRes
import xwab.core.designsystem.generated.resources.app_subtitle
import xwab.core.designsystem.generated.resources.app_title

import com.xwab.app.core.ui.components.MusicCard
import com.xwab.app.core.ui.components.SleepRelaxBackground
import com.xwab.app.core.ui.components.PlayPauseButton

@Composable
internal fun HomeScreenRoute(
    onCategoryClick: (categoryId: String) -> Unit,
    onMusicClick: (musicId: TrackId) -> Unit,
    viewModel: HomeViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onCategoryClick = onCategoryClick,
        onMusicClick = onMusicClick,
        onPlaybackClick = viewModel::togglePlayback,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeState,
    onCategoryClick: (categoryId: String) -> Unit,
    onMusicClick: (musicId: TrackId) -> Unit,
    onPlaybackClick: (musicId: TrackId) -> Unit,
) {
    SleepRelaxBackground {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(SleepRelaxTheme.dimens.categoryCardMinWidth),
            modifier = Modifier
                .widthIn(max = SleepRelaxTheme.dimens.contentMaxWidth)
                .fillMaxSize()
                .align(Alignment.Center),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                end = SleepRelaxTheme.dimens.paddingScreenHorizontal,
                top = SleepRelaxTheme.dimens.paddingScreenVertical,
                bottom = SleepRelaxTheme.dimens.spacingHuge,
            ),
            verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = stringResource(UiRes.string.app_title),
                        style = SleepRelaxTheme.typography.headlineLarge,
                        color = SleepRelaxTheme.colors.textPrimary,
                    )
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                    Text(
                        text = stringResource(UiRes.string.app_subtitle),
                        style = SleepRelaxTheme.typography.bodyLarge,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.height(SleepRelaxTheme.dimens.playIconCircleSize))
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(
                    title = stringResource(Res.string.favorites_title),
                    trailing = if (state.favoriteMusics.isEmpty()) stringResource(Res.string.favorites_hint) else null,
                )
            }

            if (state.favoriteMusics.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingSmall)) {
                        items(state.favoriteMusics, key = { it.id.value }) { music ->
                            MusicCard(
                                title = music.name,
                                subtitle = music.formattedDuration,
                                onClick = { onMusicClick(music.id) },
                                modifier = Modifier.width(SleepRelaxTheme.dimens.albumArtSize),
                                trailingContent = {
                                    PlayPauseButton(
                                        isPlaying = state.requestedTrackId == music.id && state.playIntent,
                                        onClick = { onPlaybackClick(music.id) },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
                SectionTitle(title = stringResource(Res.string.categories_title))
            }

            items(state.categories, key = { it.id }) { category ->
                CategoryCard(category, onClick = { onCategoryClick(category.id) })
            }
        }
    }
}


@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = SleepRelaxTheme.typography.bodySmall,
            color = SleepRelaxTheme.colors.accent.copy(alpha = 0.75f),
        )
        trailing?.let {
            Text(text = it, style = SleepRelaxTheme.typography.labelMedium, color = SleepRelaxTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(SleepRelaxTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(
                        SleepRelaxTheme.colors.glassWhite,
                        SleepRelaxTheme.colors.primary.copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable(onClick = onClick)
            .padding(SleepRelaxTheme.dimens.spacingLarge),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(SleepRelaxTheme.dimens.minimumTouchTarget)
                .clip(SleepRelaxTheme.shapes.small)
                .background(SleepRelaxTheme.colors.glassWhiteOverlay),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.symbol,
                color = SleepRelaxTheme.colors.accent.copy(alpha = 0.7f),
                style = SleepRelaxTheme.typography.titleLarge,
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = category.name,
                style = SleepRelaxTheme.typography.titleSmall,
                color = SleepRelaxTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
            Text(
                text = category.description,
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
                maxLines = 1,
            )
            Text(
                text = stringResource(Res.string.track_count, category.musicCount),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.accent.copy(alpha = 0.55f),
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    SleepRelaxTheme {
        HomeScreen(
            state = HomeState(
                categories = listOf(
                    Category("rain", "Rain", "Gentle raindrops", "☂", 1),
                    Category("ocean", "Ocean", "Calming waves", "≈", 1),
                ),
                favoriteMusics = listOf(
                    Music(
                        id = TrackId("gentle-rain"),
                        name = "Rain on the Window",
                        categoryId = "rain",
                        durationSeconds = 9,
                    ),
                ),
            ),
            onCategoryClick = {},
            onMusicClick = {},
            onPlaybackClick = {},
        )
    }
}
