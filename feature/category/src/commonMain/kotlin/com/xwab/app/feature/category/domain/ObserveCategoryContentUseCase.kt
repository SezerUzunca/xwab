package com.xwab.app.feature.category.domain

import com.xwab.app.core.audiocontent.MusicCatalogRepository
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import com.xwab.app.core.model.PlaybackSummary
import com.xwab.app.core.playback.PlaybackCoordinator
import com.xwab.app.core.preferences.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class CategoryContent(
    val category: Category?,
    val musics: List<Music>,
    val favoriteIds: Set<String>,
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
