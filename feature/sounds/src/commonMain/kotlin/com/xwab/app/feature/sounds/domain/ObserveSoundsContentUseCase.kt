package com.xwab.app.feature.sounds.domain

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class SoundsContent(
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
internal class ObserveSoundsContentUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(): Flow<SoundsContent> = combine(
        musicCatalog.observeCategories(),
        musicCatalog.observeAllMusic(),
        favoritesRepository.favoriteIds,
        playbackCoordinator.playback,
    ) { categories, musics, favoriteIds, playback ->
        SoundsContent(
            categories = categories,
            favoriteMusics = musics.filter { it.id in favoriteIds },
            playback = playback,
        )
    }
}
