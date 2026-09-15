package com.xwab.app.feature.favorites.domain

import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class FavoritesContent(
    val tracks: List<Track>,
    val playback: PlaybackSummary,
    val favoritesAvailable: Boolean = true,
)

/** Joins only the ports required by the user's saved-sounds capability. */
internal class ObserveFavoritesContentUseCase(
    private val soundPort: SoundPort,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) {
    private fun savedTracks(): Flow<SavedTracks> = combine(
        soundPort.observeAllTracks(),
        favoritesPort.observe(SOUND_FAVORITES_NAMESPACE),
    ) { tracks, favorites ->
        SavedTracks(tracks.filter { it.id.value in favorites.ids }, favorites.isAvailable)
    }.distinctUntilChanged()

    operator fun invoke(): Flow<FavoritesContent> = combine(
        savedTracks(),
        playbackPort.playback,
    ) { saved, playback ->
        FavoritesContent(
            tracks = saved.tracks,
            playback = playback,
            favoritesAvailable = saved.isAvailable,
        )
    }

    private data class SavedTracks(val tracks: List<Track>, val isAvailable: Boolean)
}
