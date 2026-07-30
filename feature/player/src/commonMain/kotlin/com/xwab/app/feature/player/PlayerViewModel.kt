package com.xwab.app.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.data.FavoritesRepository
import com.xwab.app.core.playback.PlaybackCoordinator
import com.xwab.app.feature.player.domain.CancelSleepTimerUseCase
import com.xwab.app.feature.player.domain.ObservePlayerContentUseCase
import com.xwab.app.feature.player.domain.SetPlaybackLoopingUseCase
import com.xwab.app.feature.player.domain.SetPlaybackVolumeUseCase
import com.xwab.app.feature.player.domain.StartSleepTimerUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class PlayerViewModel(
    private val musicId: String,
    private val observePlayerContentUseCase: ObservePlayerContentUseCase,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
    private val setPlaybackLoopingUseCase: SetPlaybackLoopingUseCase,
    private val setPlaybackVolumeUseCase: SetPlaybackVolumeUseCase,
    private val startSleepTimerUseCase: StartSleepTimerUseCase,
    private val cancelSleepTimerUseCase: CancelSleepTimerUseCase,
) : ViewModel() {
    private val playerContent = observePlayerContentUseCase(musicId)

    val state: StateFlow<PlayerState> = playerContent.map { content ->
        val selectedMusic = content.music
        val isSelectedSource = content.playback.activeSourceId == musicId
        PlayerState(
            music = selectedMusic,
            isFavorite = musicId in content.favoriteIds,
            isPlaying = isSelectedSource && content.playback.isPlaying,
            isLooping = content.playback.activeSourceId?.let { content.playback.isLooping } ?: true,
            volume = content.playback.volume,
            sleepTimerRemainingMs = content.sleepTimerRemainingMs,
            error = when {
                selectedMusic == null -> PlayerError.AudioNotFound
                isSelectedSource && content.playback.hasFailed -> PlayerError.AudioCouldNotOpen
                else -> null
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerState(),
    )

    fun toggleFavorite() {
        if (state.value.music == null) return
        viewModelScope.launch { favoritesRepository.toggle(musicId) }
    }

    fun togglePlayback() {
        viewModelScope.launch {
            val music = playerContent.first().music ?: return@launch
            playbackCoordinator.togglePlayback(music)
        }
    }

    fun setLooping(enabled: Boolean) {
        setPlaybackLoopingUseCase(enabled)
    }

    fun setVolume(volume: Float) {
        setPlaybackVolumeUseCase(volume)
    }

    fun startSleepTimer(durationMs: Long) {
        if (state.value.music != null) startSleepTimerUseCase(durationMs)
    }

    fun cancelSleepTimer() {
        cancelSleepTimerUseCase()
    }
}
