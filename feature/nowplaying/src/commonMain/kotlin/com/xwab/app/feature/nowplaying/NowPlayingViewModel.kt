package com.xwab.app.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.feature.nowplaying.domain.ObserveNowPlayingContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Presentation only. The app-scoped session survives entry and mini-player ViewModels. */
internal class NowPlayingViewModel(
    observeNowPlayingContentUseCase: ObserveNowPlayingContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<NowPlayingState> = observeNowPlayingContentUseCase()
        .map { content -> content.playback.toNowPlayingState(content.sleepTimerRemainingMs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingState())

    /** Branches on the intent rendered by the button. */
    fun togglePlayback() {
        val current = state.value
        val itemId = current.itemId ?: return
        if (current.playIntent) playbackPort.pause()
        else viewModelScope.launch { playbackPort.play(itemId) }
    }

    fun setVolume(volume: Float) {
        if (!state.value.isIdle && volume.isFinite()) playbackPort.setVolume(volume)
    }

    fun setLooping(enabled: Boolean) {
        if (!state.value.isIdle) playbackPort.setLooping(enabled)
    }

    /**
     * Allowed with nothing requested: a listener may set the timer first and pick a sound after.
     * The session's timer is not tied to an item and stops whatever is playing when it ends.
     */
    fun startSleepTimer(durationMs: Long) {
        if (durationMs > 0L) playbackPort.startSleepTimer(durationMs)
    }

    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()
}
