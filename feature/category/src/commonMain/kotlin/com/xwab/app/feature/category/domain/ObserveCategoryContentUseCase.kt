package com.xwab.app.feature.category.domain

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class CategoryContent(
    val category: Category?,
    val musics: List<Music>,
    val favoriteIds: Set<TrackId>,
    val playback: PlaybackSummary,
)

/**
 * Joins the three domain ports into the one thing a category screen shows. Feature-owned for the
 * same reason as the home screen's: only the ports it reads are shared.
 */
internal class ObserveCategoryContentUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(categoryId: String): Flow<CategoryContent> = combine(
        musicCatalog.observeCategory(categoryId),
        musicCatalog.observeMusicForCategory(categoryId),
        favoritesRepository.favoriteIds,
        playbackCoordinator.playback,
    ) { category, musics, favoriteIds, playback ->
        CategoryContent(category, musics, favoriteIds, playback)
    }
}
