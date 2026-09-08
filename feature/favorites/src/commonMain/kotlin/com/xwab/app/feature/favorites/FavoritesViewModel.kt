package com.xwab.app.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackKind
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.designsystem.state.Loadable
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
    val state: StateFlow<Loadable<FavoritesState>> = observeFavoritesContentUseCase()
        .map<FavoritesContent, Loadable<FavoritesState>> { content ->
            val playback = content.playback
            val requestedTrackId = playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId)
            val failure = playback.failure?.takeIf { it.itemId.kind == PlaybackKind.SOUND }
            Loadable.Ready(
                FavoritesState(
                    musics = content.musics,
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
            initialValue = Loadable.Loading,
        )

    fun togglePlayback(musicId: TrackId) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.requestedTrackId == musicId && current.playIntent) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId.sound(musicId.value)) }
        }
    }
}
