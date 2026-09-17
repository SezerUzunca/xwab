package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary

/**
 * What the bar draws.
 *
 * No `Loading` / `Ready` wrapper, on the same reasoning the architecture rule states: a screen with
 * nothing to wait for gives its state a default and drops the wrapper. This one has nothing to wait
 * for — a session that has never been asked for anything is not loading, it is [idle], and the bar
 * draws nothing at all.
 */
internal data class NowPlayingState(
    /** The item the session was last asked for; the thing the transport control acts on. */
    val itemId: PlaybackItemId? = null,
    /** What that item calls itself, or null while the session has not got a name for it. */
    val title: String? = null,
    /** The session's intent, not audible sound — what the button draws and what a tap branches on. */
    val playIntent: Boolean = false,
    /** Wanted, not audible yet. */
    val isPreparing: Boolean = false,
    /**
     * Why this item would not play, when that is what happened to *this* item.
     *
     * The bar can start playback, so it owes an answer when that fails, the way every list in this
     * app does for its rows. It used to drop the session's failure entirely: a tap on an
     * unreachable item bounced the icon back to play and said nothing, and the bar is the one place
     * a listener can start something from without a screen behind it to explain.
     */
    val failure: PlaybackFailure? = null,
) {
    /** Nothing has been asked for, so there is nothing to show and nothing to control. */
    val idle: Boolean get() = itemId == null
}

/**
 * Deliberately close to an identity mapping, and still worth having.
 *
 * It is where `PlaybackSummary` — another module's type — stops, so no composable in this feature
 * takes one. Every other screen in this app draws that same line.
 *
 * The failure is matched against this bar's own item rather than taken as published. A tap on a row
 * of some other screen fails into the same session, and a bar showing one item has nothing to say
 * about another item's failure.
 */
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
