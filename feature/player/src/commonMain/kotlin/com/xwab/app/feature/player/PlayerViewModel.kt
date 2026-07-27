package com.xwab.app.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.domain.usecase.CancelSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ObservePlayerContentUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackLoopingUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackVolumeUseCase
import com.xwab.app.core.domain.usecase.StartSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ToggleFavoriteUseCase
import com.xwab.app.core.domain.usecase.ToggleMusicPlaybackUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class PlayerViewModel(
    private val musicId: String,
    private val observePlayerContentUseCase: ObservePlayerContentUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleMusicPlaybackUseCase: ToggleMusicPlaybackUseCase,
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
        viewModelScope.launch { toggleFavoriteUseCase(musicId) }
    }

    fun togglePlayback() {
        viewModelScope.launch { toggleMusicPlaybackUseCase(musicId) }
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
