package com.xwab.app.feature.rehearsal

import com.xwab.app.core.rehearsal.port.RehearsalScript
import com.xwab.app.core.session.port.PlaybackFailure

/** Loading and content states owned by this feature. */
internal sealed interface RehearsalUiState {
    data object Loading : RehearsalUiState

    data class Ready(val value: RehearsalState) : RehearsalUiState
}

/**
 * The listener's script — what they typed and what the device keeps — and what the session is
 * doing with the kept one.
 */
internal data class RehearsalState(
    val draft: String = "",
    val saved: RehearsalScript? = null,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    /** The session is on the kept wording and wants it playing: what the play/pause control shows. */
    val isPlaying: Boolean = false,
    /** The kept wording is wanted but not audible yet; the first time, it is being spoken. */
    val isPreparing: Boolean = false,
    /** The session's failure for the kept wording, as the session reports it. */
    val playbackFailure: PlaybackFailure? = null,
) {
    private val savedText: String get() = saved?.text.orEmpty()

    /** There is something typed that the device does not have yet. */
    val canSave: Boolean get() = draft.trim() != savedText

    val canClear: Boolean get() = draft.isNotEmpty() || saved != null

    /** Play keeps what was typed first, so there must be something typed. */
    val canPlay: Boolean get() = isPlaying || draft.isNotBlank()
}

internal enum class SaveStatus { Idle, Saved, Failed }
