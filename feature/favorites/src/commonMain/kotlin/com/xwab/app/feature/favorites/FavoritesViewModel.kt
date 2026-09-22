package com.xwab.app.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.feature.favorites.domain.FavoritesContent
import com.xwab.app.feature.favorites.domain.ObserveFavoritesContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class FavoritesViewModel(
    observeFavoritesContentUseCase: ObserveFavoritesContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    // The port's per-collection fallback may be empty after an upstream restart.
    private var lastKnownTracks: List<Track>? = null

    val state: StateFlow<FavoritesUiState> = observeFavoritesContentUseCase()
        .map<FavoritesContent, FavoritesUiState> { content ->
            if (content.favoritesAvailable) lastKnownTracks = content.tracks
            val tracks = lastKnownTracks ?: content.tracks
            val playback = content.playback
            val requestedTrackId = playback.requestedValueOf(SOUND_PLAYBACK_KIND)?.let(::TrackId)
                ?.takeIf { id -> tracks.any { it.id == id } }
            val failure = playback.failure?.takeIf { failure ->
                failure.itemId.kind == SOUND_PLAYBACK_KIND && tracks.any { it.id.value == failure.itemId.value }
            }
            FavoritesUiState.Ready(
                FavoritesState(
                    tracks = tracks,
                    favoritesAvailable = content.favoritesAvailable,
                    requestedTrackId = requestedTrackId,
                    playIntent = requestedTrackId != null && playback.playIntent,
                    isPreparing = requestedTrackId != null && playback.isPreparing,
                    playbackFailure = failure,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading,
        )

    fun togglePlayback(trackId: TrackId) {
        val current = (state.value as? FavoritesUiState.Ready)?.value ?: return
        if (current.isRowPlaying(trackId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId(SOUND_PLAYBACK_KIND, trackId.value)) }
        }
    }
}
