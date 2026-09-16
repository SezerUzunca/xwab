package com.xwab.app.feature.nowplaying

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
) {
    /** Nothing has been asked for, so there is nothing to show and nothing to control. */
    val idle: Boolean get() = itemId == null
}

/**
 * Deliberately close to an identity mapping, and still worth having.
 *
 * It is where `PlaybackSummary` — another module's type — stops, so no composable in this feature
 * takes one. Every other screen in this app draws that same line.
 */
internal fun PlaybackSummary.toNowPlayingState(): NowPlayingState = NowPlayingState(
    itemId = requestedItemId,
    title = title,
    playIntent = playIntent,
    isPreparing = isPreparing,
)
