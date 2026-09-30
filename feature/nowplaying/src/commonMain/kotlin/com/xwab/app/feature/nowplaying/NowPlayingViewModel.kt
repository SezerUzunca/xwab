package com.xwab.app.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Presentation only. The app-scoped session outlives the bar's ViewModel. */
internal class NowPlayingViewModel(
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<NowPlayingUiState> = playbackPort.playback
        .map<PlaybackSummary, NowPlayingUiState> { NowPlayingUiState.Ready(it.toNowPlayingState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NowPlayingUiState.Loading,
        )

    /** The session's timer; kept out of [state] because it ticks every second. */
    val sleepTimerRemainingMs: StateFlow<Long?> = playbackPort.sleepTimerRemainingMs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    /** Branches on the intent rendered by the button. */
    fun togglePlayback() {
        val current = readyState() ?: return
        val itemId = current.itemId ?: return
        if (current.playIntent) playbackPort.pause()
        else viewModelScope.launch { playbackPort.play(itemId) }
    }

    /**
     * The one setting the bar itself offers: a timer running with nothing requested has no item
     * screen to be cancelled from.
     */
    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()

    /** What the bar is showing, or null while the first content has not arrived. */
    private fun readyState(): NowPlayingState? = (state.value as? NowPlayingUiState.Ready)?.value
}
