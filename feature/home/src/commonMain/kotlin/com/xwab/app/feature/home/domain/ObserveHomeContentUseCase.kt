package com.xwab.app.feature.home.domain

import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import com.xwab.app.core.domain.port.PlaybackSummary
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class HomeContent(
    val categories: List<Category>,
    val favoriteMusics: List<Music>,
    val playback: PlaybackSummary,
)

/**
 * Joins the three domain ports into the one thing the home screen shows.
 *
 * Lives in the feature rather than in `core:domain` because no other screen asks this question:
 * only the ports it reads are shared, and those are what `core:domain` publishes.
 */
internal class ObserveHomeContentUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(): Flow<HomeContent> = combine(
        musicCatalog.observeCategories(),
        musicCatalog.observeAllMusic(),
        favoritesRepository.favoriteIds,
        playbackCoordinator.playback,
    ) { categories, musics, favoriteIds, playback ->
        HomeContent(
            categories = categories,
            favoriteMusics = musics.filter { it.id in favoriteIds },
            playback = playback,
        )
    }
}
