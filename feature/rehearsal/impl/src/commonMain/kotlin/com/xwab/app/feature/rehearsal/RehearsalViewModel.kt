package com.xwab.app.feature.rehearsal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.rehearsal.port.REHEARSAL_PLAYBACK_KIND
import com.xwab.app.core.rehearsal.port.REHEARSAL_SCRIPT_MAX_LENGTH
import com.xwab.app.core.rehearsal.port.RehearsalPort
import com.xwab.app.core.rehearsal.port.RehearsalSaveResult
import com.xwab.app.core.rehearsal.port.RehearsalScriptId
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.requestedValueOf
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class RehearsalViewModel(
    private val rehearsalPort: RehearsalPort,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    /**
     * What the field shows, set in the same call as the keystroke: a text field whose value comes
     * back a frame later through [state] moves its cursor and scrambles fast typing. It follows the
     * kept script until the listener types, and again once what they typed is kept.
     */
    private val draftText = MutableStateFlow("")
    val draft: StateFlow<String> = draftText.asStateFlow()
    private var followsKept = true
    private val saveStatus = MutableStateFlow(SaveStatus.Idle)

    val state: StateFlow<RehearsalUiState> = combine(
        rehearsalPort.observeScript(),
        playbackPort.playback,
        draftText,
        saveStatus,
    ) { saved, playback, typed, status ->
        // Only the kept wording is this screen's: an earlier one the session still names is not.
        val onSaved = saved != null && playback.requestedValueOf(REHEARSAL_PLAYBACK_KIND) == saved.id.value
        RehearsalUiState.Ready(RehearsalState(
            draft = typed,
            saved = saved,
            saveStatus = status,
            isPlaying = onSaved && playback.playIntent,
            isPreparing = onSaved && playback.isPreparing,
            playbackFailure = playback.failure?.takeIf { saved != null && it.itemId == playbackItemOf(saved.id) },
        ))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RehearsalUiState.Loading,
    )

    init {
        viewModelScope.launch {
            rehearsalPort.observeScript().collect { kept ->
                if (followsKept) draftText.value = kept?.text.orEmpty()
            }
        }
    }

    fun onDraftChange(text: String) {
        followsKept = false
        draftText.value = text.take(REHEARSAL_SCRIPT_MAX_LENGTH)
        saveStatus.value = SaveStatus.Idle
    }

    fun save() {
        val current = ready() ?: return
        viewModelScope.launch { store(current.draft) }
    }

    /** Removes the script; one that is playing stops, since it is the thing being thrown away. */
    fun clear() {
        val current = ready() ?: return
        if (current.isPlaying) playbackPort.pause()
        onDraftChange("")
        viewModelScope.launch { store("") }
    }

    /**
     * Branches on the value the control renders, so the icon and the tap cannot disagree. A play
     * keeps what was typed first: the listener hears the words on the screen, never an older wording.
     */
    fun togglePlayback() {
        val current = ready() ?: return
        when {
            current.isPlaying -> playbackPort.pause()
            current.draft.isNotBlank() -> viewModelScope.launch { keepAndPlay(current) }
        }
    }

    private suspend fun keepAndPlay(current: RehearsalState) {
        val kept = !current.canSave || store(current.draft)
        val script = if (kept) rehearsalPort.observeScript().first() else null
        script?.let { playbackPort.play(playbackItemOf(it.id)) }
    }

    /** Saves [text] and reports whether the device kept it; a refusal leaves the field as typed. */
    private suspend fun store(text: String): Boolean {
        val saved = rehearsalPort.save(text) == RehearsalSaveResult.Saved
        // The field shows what was kept, trimmed, unless the listener typed on while it was saved.
        if (saved && draftText.compareAndSet(text, text.trim())) followsKept = true
        saveStatus.value = if (saved) SaveStatus.Saved else SaveStatus.Failed
        return saved
    }

    /** The screen's state with the field as it is now, which [state] may not have caught up with. */
    private fun ready(): RehearsalState? =
        (state.value as? RehearsalUiState.Ready)?.value?.copy(draft = draftText.value)
}

internal fun playbackItemOf(id: RehearsalScriptId) = PlaybackItemId(REHEARSAL_PLAYBACK_KIND, id.value)
