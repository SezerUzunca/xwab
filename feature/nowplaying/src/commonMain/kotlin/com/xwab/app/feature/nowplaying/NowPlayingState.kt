package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary

/** Loading and content states owned by this feature. */
internal sealed interface NowPlayingUiState {
    data object Loading : NowPlayingUiState

    data class Ready(val value: NowPlayingState) : NowPlayingUiState
}

/** Content available in [NowPlayingUiState.Ready]. Idle — nothing requested — is valid content. */
internal data class NowPlayingState(
    /** The item the session was last asked for; the thing the transport control acts on. */
    val itemId: PlaybackItemId? = null,
    /** What that item calls itself, or null while the session has not got a name for it. */
    val title: String? = null,
    /** The session's intent, not audible sound — what the button draws and what a tap branches on. */
    val playIntent: Boolean = false,
    /** Wanted, not audible yet. */
    val isPreparing: Boolean = false,
    /** Only a failure belonging to the item rendered by these controls. */
    val failure: PlaybackFailure? = null,
) {
    /** Nothing requested: the bar shows only a running timer, if there is one. */
    val isIdle: Boolean get() = itemId == null

    /**
     * Whether the bar belongs on screen beside [shownItem], the item whose own screen is showing.
     *
     * The item's own screen already carries its play/pause and timer, so the bar steps aside for it
     * and for no other item. With nothing requested, a running timer keeps the bar so it can be
     * cancelled — except on an item's screen, which already draws that timer with its cancel.
     */
    fun showsBarBeside(shownItem: PlaybackItemId?, sleepTimerRemainingMs: Long?): Boolean =
        if (isIdle) sleepTimerRemainingMs != null && shownItem == null else itemId != shownItem
}

/** Keeps core summary types out of composables and excludes another item's failure. */
internal fun PlaybackSummary.toNowPlayingState(): NowPlayingState {
    val itemId = requestedItemId
    return NowPlayingState(
        itemId = itemId,
        title = title,
        playIntent = playIntent,
        isPreparing = isPreparing,
        failure = failure?.takeIf { it.itemId == itemId },
    )
}
