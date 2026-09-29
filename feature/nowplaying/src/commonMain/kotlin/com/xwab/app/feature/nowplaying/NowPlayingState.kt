package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary

/** Feature-owned presentation shared by the mini player and entry screen. Idle is a valid state. */
internal data class NowPlayingState(
    /** The item the session was last asked for; the thing the transport control acts on. */
    val itemId: PlaybackItemId? = null,
    /** What that item calls itself, or null while the session has not got a name for it. */
    val title: String? = null,
    /** The session's intent, not audible sound — what the button draws and what a tap branches on. */
    val playIntent: Boolean = false,
    /** Wanted, not audible yet. */
    val isPreparing: Boolean = false,
    val isLooping: Boolean = false,
    val volume: Float = 1f,
    val sleepTimerRemainingMs: Long? = null,
    /** Only a failure belonging to the item rendered by these controls. */
    val failure: PlaybackFailure? = null,
) {
    /** Nothing requested: the player screen shows its empty state and the timer. */
    val isIdle: Boolean get() = itemId == null

    /** A running timer keeps the mini player on screen even with nothing requested, so it can be cancelled. */
    val showsMiniPlayer: Boolean get() = !isIdle || sleepTimerRemainingMs != null

    /**
     * Whether the bar belongs on screen beside [shownItem], the item whose own screen is showing.
     * That screen already carries this item's play/pause and timer, so the bar steps aside for it
     * and for no other item.
     */
    fun showsMiniPlayerBeside(shownItem: PlaybackItemId?): Boolean =
        showsMiniPlayer && (shownItem == null || itemId != shownItem)
}

/** Keeps core summary types out of composables and excludes another item's failure. */
internal fun PlaybackSummary.toNowPlayingState(remainingMs: Long? = null): NowPlayingState {
    val itemId = requestedItemId
    return NowPlayingState(
        itemId = itemId,
        title = title,
        playIntent = playIntent,
        isPreparing = isPreparing,
        isLooping = isLooping,
        volume = volume,
        sleepTimerRemainingMs = remainingMs,
        failure = failure?.takeIf { it.itemId == itemId },
    )
}
