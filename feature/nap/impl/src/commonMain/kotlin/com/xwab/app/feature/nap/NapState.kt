package com.xwab.app.feature.nap

import com.xwab.app.core.nap.port.NAP_PLAYBACK_KIND
import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId

/** Loading and content states owned by this feature. */
internal sealed interface NapUiState {
    data object Loading : NapUiState

    data class Ready(val value: NapState) : NapUiState
}

/** The naps in [NapUiState.Ready], and what the session is doing with them. */
internal data class NapState(
    val naps: List<Nap> = emptyList(),
    /** The nap the session was last asked for, or null when it is on something else or nothing. */
    val requestedNapId: NapId? = null,
    /** What a nap's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested nap is wanted but not audible yet; the first time, it is being made. */
    val isPreparing: Boolean = false,
    /** The session's failure for a nap, as the session reports it. */
    val playbackFailure: PlaybackFailure? = null,
) {
    fun isNapPlaying(napId: NapId): Boolean = requestedNapId == napId && playIntent

    fun isNapPreparing(napId: NapId): Boolean = requestedNapId == napId && isPreparing

    fun napFailure(napId: NapId): PlaybackFailure? = playbackFailure?.takeIf { it.itemId == playbackItemOf(napId) }
}

internal fun playbackItemOf(napId: NapId) = PlaybackItemId(NAP_PLAYBACK_KIND, napId.value)
