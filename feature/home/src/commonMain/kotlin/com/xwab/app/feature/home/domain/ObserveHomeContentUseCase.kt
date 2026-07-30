package com.xwab.app.feature.home.domain

import com.xwab.app.core.data.FavoritesRepository
import com.xwab.app.core.data.MusicCatalogRepository
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import com.xwab.app.core.model.PlaybackSummary
import com.xwab.app.core.playback.PlaybackCoordinator
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
 * Lives in the feature because no other screen asks this question; only its data and playback
 * contracts are shared.
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
