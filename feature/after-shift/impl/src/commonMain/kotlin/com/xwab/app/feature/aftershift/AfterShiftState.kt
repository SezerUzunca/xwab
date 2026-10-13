package com.xwab.app.feature.aftershift

import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId

/** Loading and content states owned by this feature. */
internal sealed interface AfterShiftUiState {
    data object Loading : AfterShiftUiState

    data class Ready(val value: AfterShiftState) : AfterShiftUiState
}

/** The guides in [AfterShiftUiState.Ready], and what the session is doing with them. */
internal data class AfterShiftState(
    val guides: List<Guide> = emptyList(),
    /** The guide the session was last asked for, or null when it is on something else or nothing. */
    val requestedGuideId: GuideId? = null,
    /** What a guide's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested guide is wanted but not audible yet; the first time, it is being spoken. */
    val isPreparing: Boolean = false,
    /**
     * The session's failure, carried as the session reports it. A failed lookup releases the
     * session's claim, so the guide it belongs to is found through [guideFailure].
     */
    val playbackFailure: PlaybackFailure? = null,
) {
    /** Whether any guide is wanted at all: what a timer set on this screen asks before starting one. */
    val isAnyGuidePlaying: Boolean get() = requestedGuideId != null && playIntent

    fun isGuidePlaying(guideId: GuideId): Boolean = requestedGuideId == guideId && playIntent

    fun isGuidePreparing(guideId: GuideId): Boolean = requestedGuideId == guideId && isPreparing

    fun guideFailure(guideId: GuideId): PlaybackFailure? =
        playbackFailure?.takeIf { it.itemId == playbackItemOf(guideId) }
}

/**
 * The note the listener parks for after they wake: what they typed, what is stored, and how the
 * last save went. The draft is the screen's; the stored text is the device's.
 */
internal data class ParkedNote(
    val draft: String = "",
    val saved: String = "",
    val status: NoteStatus = NoteStatus.Idle,
) {
    /** There is something typed that the device does not have yet. */
    val canSave: Boolean get() = draft.trim() != saved.trim()

    val canClear: Boolean get() = draft.isNotEmpty() || saved.isNotEmpty()
}

internal enum class NoteStatus { Idle, Saved, Failed }

internal fun playbackItemOf(guideId: GuideId) = PlaybackItemId(GUIDE_PLAYBACK_KIND, guideId.value)
