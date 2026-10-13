package com.xwab.app.feature.shuffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.feature.shuffle.domain.ObserveShuffleContentUseCase
import com.xwab.app.feature.shuffle.domain.ShuffleContent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class ShuffleViewModel(
    observeShuffleContentUseCase: ObserveShuffleContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<ShuffleUiState> = observeShuffleContentUseCase()
        .map<ShuffleContent, ShuffleUiState> { content ->
            val playback = content.playback
            // This screen shows decks, so the session being on a sound is the same to it as the
            // session being on nothing: no deck here is the current item.
            val requestedDeckId = playback.requestedValueOf(SHUFFLE_PLAYBACK_KIND)?.let(::ShuffleDeckId)
            ShuffleUiState.Ready(ShuffleState(
                decks = content.decks,
                requestedDeckId = requestedDeckId,
                playIntent = requestedDeckId != null && playback.playIntent,
                isPreparing = requestedDeckId != null && playback.isPreparing,
                playbackFailure = playback.failure?.takeIf { it.itemId.kind == SHUFFLE_PLAYBACK_KIND },
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ShuffleUiState.Loading,
        )

    /** The session's timer; kept out of [state] because it ticks every second. */
    val sleepTimerRemainingMs: StateFlow<Long?> =
        playbackPort.sleepTimerRemainingMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(deckId: ShuffleDeckId) {
        val current = (state.value as? ShuffleUiState.Ready)?.value ?: return
        if (current.isDeckPlaying(deckId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(playbackItemOf(deckId)) }
        }
    }

    /**
     * Starts the session's timer and, when no deck is playing, the first one: a timer set beside
     * the words is for the words. Restarting it while a deck plays leaves playback alone.
     */
    fun startSleepTimer(durationMs: Long) {
        if (durationMs <= 0L) return
        playbackPort.startSleepTimer(durationMs)
        val current = (state.value as? ShuffleUiState.Ready)?.value ?: return
        val first = current.decks.firstOrNull()
        if (first != null && !current.isAnyDeckPlaying) {
            viewModelScope.launch { playbackPort.play(playbackItemOf(first.id)) }
        }
    }

    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()
}
