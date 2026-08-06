package com.xwab.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.feature.home.domain.ObserveHomeContentUseCase
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
                playingMusicId = content.playback.trackId,
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
        if (current.playingMusicId == musicId && current.playIntent) {
            playbackCoordinator.pause()
        } else {
            viewModelScope.launch { playbackCoordinator.play(musicId) }
        }
    }
}
