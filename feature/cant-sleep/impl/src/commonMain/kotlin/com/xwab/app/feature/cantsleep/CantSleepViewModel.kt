package com.xwab.app.feature.cantsleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.feature.cantsleep.domain.CantSleepContent
import com.xwab.app.feature.cantsleep.domain.ObserveCantSleepContentUseCase
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
internal class CantSleepViewModel(
    observeCantSleepContentUseCase: ObserveCantSleepContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<CantSleepUiState> = observeCantSleepContentUseCase()
        .map<CantSleepContent, CantSleepUiState> { content ->
            val playback = content.playback
            // Only this screen's guides are its business: the session being on a wind-down guide
            // is the same to it as the session being on a sound.
            val ownIds = content.guides.map { it.id }.toSet()
            val requestedGuideId = playback.requestedValueOf(GUIDE_PLAYBACK_KIND)?.let(::GuideId)
                ?.takeIf { it in ownIds }
            CantSleepUiState.Ready(CantSleepState(
                guides = content.guides,
                requestedGuideId = requestedGuideId,
                playIntent = requestedGuideId != null && playback.playIntent,
                isPreparing = requestedGuideId != null && playback.isPreparing,
                playbackFailure = playback.failure?.takeIf { failure ->
                    ownIds.any { failure.itemId == playbackItemOf(it) }
                },
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CantSleepUiState.Loading,
        )

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(guideId: GuideId) {
        val current = (state.value as? CantSleepUiState.Ready)?.value ?: return
        if (current.isGuidePlaying(guideId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(playbackItemOf(guideId)) }
        }
    }
}
