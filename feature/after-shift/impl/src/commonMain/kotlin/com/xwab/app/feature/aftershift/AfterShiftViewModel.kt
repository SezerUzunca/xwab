package com.xwab.app.feature.aftershift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.notes.port.NoteSaveResult
import com.xwab.app.core.notes.port.NotesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.feature.aftershift.domain.AfterShiftContent
import com.xwab.app.feature.aftershift.domain.ObserveAfterShiftContentUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class AfterShiftViewModel(
    observeAfterShiftContentUseCase: ObserveAfterShiftContentUseCase,
    private val playbackPort: PlaybackPort,
    private val notesPort: NotesPort,
) : ViewModel() {
    val state: StateFlow<AfterShiftUiState> = observeAfterShiftContentUseCase()
        .map<AfterShiftContent, AfterShiftUiState> { content ->
            val playback = content.playback
            // This screen shows guides, so the session being on a sound is the same to it as the
            // session being on nothing: no guide here is the current item.
            val requestedGuideId = playback.requestedValueOf(GUIDE_PLAYBACK_KIND)?.let(::GuideId)
            AfterShiftUiState.Ready(AfterShiftState(
                guides = content.guides,
                requestedGuideId = requestedGuideId,
                playIntent = requestedGuideId != null && playback.playIntent,
                isPreparing = requestedGuideId != null && playback.isPreparing,
                playbackFailure = playback.failure?.takeIf { it.itemId.kind == GUIDE_PLAYBACK_KIND },
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AfterShiftUiState.Loading,
        )

    /** The session's timer; kept out of [state] because it ticks every second. */
    val sleepTimerRemainingMs: StateFlow<Long?> =
        playbackPort.sleepTimerRemainingMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val parkedNote = MutableStateFlow(ParkedNote())
    val note: StateFlow<ParkedNote> = parkedNote.asStateFlow()

    init {
        // The stored note fills the field, unless the listener has started typing over it.
        viewModelScope.launch {
            notesPort.observe(AFTER_SHIFT_NOTE).collect { stored ->
                parkedNote.update { current ->
                    val untouched = current.draft == current.saved
                    current.copy(saved = stored, draft = if (untouched) stored else current.draft)
                }
            }
        }
    }

    fun onNoteChange(text: String) {
        parkedNote.update { it.copy(draft = text, status = NoteStatus.Idle) }
    }

    fun saveNote() = store(parkedNote.value.draft)

    fun clearNote() {
        parkedNote.update { it.copy(draft = "") }
        store("")
    }

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(guideId: GuideId) {
        val current = (state.value as? AfterShiftUiState.Ready)?.value ?: return
        if (current.isGuidePlaying(guideId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(playbackItemOf(guideId)) }
        }
    }

    /**
     * Starts the session's timer and, when no guide is playing, the first one: a timer set beside
     * the guides is for winding down. Restarting it while a guide plays leaves playback alone.
     */
    fun startSleepTimer(durationMs: Long) {
        if (durationMs <= 0L) return
        playbackPort.startSleepTimer(durationMs)
        val current = (state.value as? AfterShiftUiState.Ready)?.value ?: return
        val first = current.guides.firstOrNull()
        if (first != null && !current.isAnyGuidePlaying) {
            viewModelScope.launch { playbackPort.play(playbackItemOf(first.id)) }
        }
    }

    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()

    private fun store(text: String) {
        viewModelScope.launch {
            val result = notesPort.save(AFTER_SHIFT_NOTE, text.trim())
            parkedNote.update { current ->
                current.copy(status = if (result == NoteSaveResult.Saved) NoteStatus.Saved else NoteStatus.Failed)
            }
        }
    }
}

/**
 * The note's key on disk. Renaming it strands what listeners have parked, so it changes only with a
 * migration; `AfterShiftViewModelTest` pins it.
 */
internal const val AFTER_SHIFT_NOTE = "after-shift"
