package com.xwab.app.feature.sounds.domain

import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
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
    private val soundCatalogPort: SoundCatalogPort,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(musicId: TrackId): Flow<PlayerContent> = combine(
        soundCatalogPort.observeMusic(musicId),
        favoritesPort.favoriteIds,
        playbackPort.playback,
        playbackPort.sleepTimerRemainingMs,
    ) { music, favoriteIds, playback, sleepTimerRemainingMs ->
        PlayerContent(
            music = music,
            favoriteIds = favoriteIds,
            playback = playback,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
        )
    }
}
