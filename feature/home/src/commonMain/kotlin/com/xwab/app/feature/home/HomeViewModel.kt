package com.xwab.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.playback.PlaybackCoordinator
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
                playingMusicId = content.playback.activeSourceId,
                isPlaying = content.playback.isPlaying,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState(),
        )

    fun togglePlayback(musicId: String) {
        val music = state.value.favoriteMusics.firstOrNull { it.id == musicId } ?: return
        viewModelScope.launch { playbackCoordinator.togglePlayback(music) }
    }
}
