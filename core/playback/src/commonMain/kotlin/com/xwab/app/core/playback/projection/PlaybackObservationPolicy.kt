package com.xwab.app.core.playback.projection

/**
 * Whether the native player still owes the engine a transition it cannot be told about.
 *
 * AVFoundation announces the ends of playback — an item that finished, failed, or stalled — through
 * notifications, but exposes readiness and the play/wait distinction only as properties. The engine
 * therefore watches those properties while a transition is actually outstanding, and not at all once
 * the player has settled: an item that is playing, or ready and paused, leaves that state through a
 * notification or through a command this engine itself issued.
 *
 * [playTransitionTicksRemaining] covers the one gap in that reasoning. `play()` does not move
 * `timeControlStatus` synchronously, so a player that still reads as settled immediately after a
 * play command has to be watched for a short while rather than trusted.
 */
internal fun shouldObserveEngineTransition(
    hasCurrentItem: Boolean,
    hasFailure: Boolean,
    isReadyToPlay: Boolean,
    isWaitingToPlay: Boolean,
    playTransitionTicksRemaining: Int,
): Boolean = when {
    !hasCurrentItem || hasFailure -> false
    !isReadyToPlay || isWaitingToPlay -> true
    else -> playTransitionTicksRemaining > 0
}
