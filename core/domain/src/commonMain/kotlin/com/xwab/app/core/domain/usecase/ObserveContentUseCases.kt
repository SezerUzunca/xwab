package com.xwab.app.core.domain.usecase

import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import com.xwab.app.core.domain.port.PlaybackSummary
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeContent(
    val categories: List<Category>,
    val favoriteMusics: List<Music>,
    val playback: PlaybackSummary,
)

class ObserveHomeContentUseCase(
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

data class CategoryContent(
    val category: Category?,
    val musics: List<Music>,
    val favoriteIds: Set<String>,
    val playback: PlaybackSummary,
)

class ObserveCategoryContentUseCase(
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

data class PlayerContent(
    val music: Music?,
    val favoriteIds: Set<String>,
    val playback: PlaybackSummary,
    val sleepTimerRemainingMs: Long?,
)

class ObservePlayerContentUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(musicId: String): Flow<PlayerContent> = combine(
        musicCatalog.observeMusic(musicId),
        favoritesRepository.favoriteIds,
        playbackCoordinator.playback,
        playbackCoordinator.sleepTimerRemainingMs,
    ) { music, favoriteIds, playback, sleepTimerRemainingMs ->
        PlayerContent(
            music = music,
            favoriteIds = favoriteIds,
            playback = playback,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
        )
    }
}
