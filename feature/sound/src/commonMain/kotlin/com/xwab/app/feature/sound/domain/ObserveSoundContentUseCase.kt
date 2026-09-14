package com.xwab.app.feature.sound.domain

import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class SoundContent(
    val track: Track?,
    val favoriteIds: Set<TrackId>,
    val playback: PlaybackSummary,
    val sleepTimerRemainingMs: Long?,
)

/**
 * Joins the three domain ports plus the sleep timer into the one thing the sound screen shows.
 * Feature-owned for the same reason as the other screen-owned use cases: only the ports it reads
 * are shared.
 */
internal class ObserveSoundContentUseCase(
    private val soundPort: SoundPort,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(trackId: TrackId): Flow<SoundContent> = combine(
        soundPort.observeTrack(trackId),
        favoritesPort.observe(SOUND_FAVORITES_NAMESPACE).map { ids -> ids.mapTo(mutableSetOf(), ::TrackId) },
        playbackPort.playback,
        playbackPort.sleepTimerRemainingMs,
    ) { track, favoriteIds, playback, sleepTimerRemainingMs ->
        SoundContent(
            track = track,
            favoriteIds = favoriteIds,
            playback = playback,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
        )
    }
}
