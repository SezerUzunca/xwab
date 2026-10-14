package com.xwab.app.feature.nap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.nap.port.NAP_PLAYBACK_KIND
import com.xwab.app.core.nap.port.NapId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.feature.nap.domain.NapContent
import com.xwab.app.feature.nap.domain.ObserveNapContentUseCase
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
internal class NapViewModel(
    observeNapContentUseCase: ObserveNapContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<NapUiState> = observeNapContentUseCase()
        .map<NapContent, NapUiState> { content ->
            val playback = content.playback
            val requestedNapId = playback.requestedValueOf(NAP_PLAYBACK_KIND)?.let(::NapId)
            NapUiState.Ready(NapState(
                naps = content.naps,
                requestedNapId = requestedNapId,
                playIntent = requestedNapId != null && playback.playIntent,
                isPreparing = requestedNapId != null && playback.isPreparing,
                playbackFailure = playback.failure?.takeIf { it.itemId.kind == NAP_PLAYBACK_KIND },
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NapUiState.Loading,
        )

    /**
     * Branches on the value the control renders, so the icon and the tap cannot disagree. Playing a
     * nap cancels the session's sleep timer: a nap ends with its wake-up, and a timer running out
     * first would stop it before the voice comes.
     */
    fun togglePlayback(napId: NapId) {
        val current = (state.value as? NapUiState.Ready)?.value ?: return
        if (current.isNapPlaying(napId)) {
            playbackPort.pause()
        } else {
            playbackPort.cancelSleepTimer()
            viewModelScope.launch { playbackPort.play(playbackItemOf(napId)) }
        }
    }
}
