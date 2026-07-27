package com.xwab.app.core.domain.usecase

import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import com.xwab.app.core.repository.FavoritesRepository
import com.xwab.app.core.repository.MusicRepository
import com.xwab.app.core.playback.PlaybackCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeContent(
    val categories: List<Category>,
    val favoriteMusics: List<Music>,
    val playback: PlaybackSummary,
)

class ObserveHomeContentUseCase(
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(): Flow<HomeContent> = combine(
        musicRepository.getCategories(),
        musicRepository.getAllMusic(),
        favoritesRepository.favoriteIds,
        playbackCoordinator.state,
    ) { categories, musics, favoriteIds, playback ->
        HomeContent(
            categories = categories,
            favoriteMusics = musics.filter { it.id in favoriteIds },
            playback = playback.toPlaybackSummary(),
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
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(categoryId: String): Flow<CategoryContent> = combine(
        musicRepository.getCategory(categoryId),
        musicRepository.getMusicForCategory(categoryId),
        favoritesRepository.favoriteIds,
        playbackCoordinator.state,
    ) { category, musics, favoriteIds, playback ->
        CategoryContent(category, musics, favoriteIds, playback.toPlaybackSummary())
    }
}

data class PlayerContent(
    val music: Music?,
    val favoriteIds: Set<String>,
    val playback: PlaybackSummary,
    val sleepTimerRemainingMs: Long?,
)

class ObservePlayerContentUseCase(
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(musicId: String): Flow<PlayerContent> = combine(
        musicRepository.getMusic(musicId),
        favoritesRepository.favoriteIds,
        playbackCoordinator.state,
        playbackCoordinator.sleepTimerState,
    ) { music, favoriteIds, playback, sleepTimerState ->
        PlayerContent(
            music = music,
            favoriteIds = favoriteIds,
            playback = playback.toPlaybackSummary(),
            sleepTimerRemainingMs = sleepTimerState.remainingMs,
        )
    }
}
