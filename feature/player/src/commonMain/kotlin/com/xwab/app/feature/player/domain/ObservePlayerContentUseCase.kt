package com.xwab.app.feature.player.domain

import com.xwab.app.core.audiocontent.MusicCatalogRepository
import com.xwab.app.core.model.Music
import com.xwab.app.core.model.PlaybackSummary
import com.xwab.app.core.playback.PlaybackCoordinator
import com.xwab.app.core.preferences.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class PlayerContent(
    val music: Music?,
    val favoriteIds: Set<String>,
    val playback: PlaybackSummary,
    val sleepTimerRemainingMs: Long?,
)

/**
 * Joins the three domain ports plus the sleep timer into the one thing the player screen shows.
 * Feature-owned for the same reason as the home screen's: only the ports it reads are shared.
 */
internal class ObservePlayerContentUseCase(
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
