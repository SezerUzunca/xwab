package com.xwab.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackKind
import com.xwab.app.core.playbacksession.requestedValueOf
import com.xwab.app.home.domain.ObserveHomeContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    observeHomeContentUseCase: ObserveHomeContentUseCase,
    private val playbackCoordinator: PlaybackCoordinator,
) : ViewModel() {
    val state: StateFlow<HomeState> = observeHomeContentUseCase()
        .map { content ->
            HomeState(
                categories = content.categories,
                favoriteMusics = content.favoriteMusics,
                // This screen lists sounds, so the session being on a story is the same to it as
                // the session being on nothing: no row here is the current item.
                requestedTrackId = content.playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId),
                playIntent = content.playback.playIntent,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState(),
        )

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(musicId: TrackId) {
        val current = state.value
        if (current.requestedTrackId == musicId && current.playIntent) {
            playbackCoordinator.pause()
        } else {
            viewModelScope.launch { playbackCoordinator.play(PlaybackItemId.sound(musicId.value)) }
        }
    }
}
