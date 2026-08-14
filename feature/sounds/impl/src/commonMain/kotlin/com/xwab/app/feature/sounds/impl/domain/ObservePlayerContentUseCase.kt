package com.xwab.app.feature.sounds.impl.domain

import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class PlayerContent(
    val music: Music?,
    val favoriteIds: Set<TrackId>,
    val playback: PlaybackSummary,
    val sleepTimerRemainingMs: Long?,
)

/**
 * Joins the three domain ports plus the sleep timer into the one thing the player screen shows.
 * Feature-owned for the same reason as the other screen-owned use cases: only the ports it reads
 * are shared.
 */
internal class ObservePlayerContentUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(musicId: TrackId): Flow<PlayerContent> = combine(
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
