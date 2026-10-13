package com.xwab.app.feature.cantsleep

import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId

/** Loading and content states owned by this feature. */
internal sealed interface CantSleepUiState {
    data object Loading : CantSleepUiState

    data class Ready(val value: CantSleepState) : CantSleepUiState
}

/** The out-of-bed guides in [CantSleepUiState.Ready], and what the session is doing with them. */
internal data class CantSleepState(
    val guides: List<Guide> = emptyList(),
    /** The guide of this screen the session was last asked for, or null when it is on anything else. */
    val requestedGuideId: GuideId? = null,
    /** What a guide's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested guide is wanted but not audible yet; the first time, it is being spoken. */
    val isPreparing: Boolean = false,
    /** The session's failure for one of this screen's guides, as the session reports it. */
    val playbackFailure: PlaybackFailure? = null,
) {
    fun isGuidePlaying(guideId: GuideId): Boolean = requestedGuideId == guideId && playIntent

    fun isGuidePreparing(guideId: GuideId): Boolean = requestedGuideId == guideId && isPreparing

    fun guideFailure(guideId: GuideId): PlaybackFailure? =
        playbackFailure?.takeIf { it.itemId == playbackItemOf(guideId) }
}

internal fun playbackItemOf(guideId: GuideId) = PlaybackItemId(GUIDE_PLAYBACK_KIND, guideId.value)
