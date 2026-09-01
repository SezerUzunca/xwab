package com.xwab.app.core.playbacksession

import kotlinx.coroutines.flow.Flow

/**
 * Control over the single playback session the app runs.
 *
 * Not a repository: there is nothing to store or query here, only a live session to steer.
 *
 * Everything it publishes is playback-capability-owned. The engine's own state model stays
 * behind the adapter, which keeps engine details away from features.
 *
 * ## Threading
 *
 * **Every member here must be called from the main thread**, and [play] must be *resumed* on it too
 * — so launch it in a main-dispatched scope, which is what a `componentScope()` is. The commands go
 * straight to `core:playback:engine`, whose Media3 and AVFoundation facades are main-thread-only and
 * check it on entry: calling from elsewhere fails loudly rather than corrupting the session, but it
 * fails deep in the engine instead of here. The constraint is stated on this port so it is part of
 * the contract a caller reads, not a property of an implementation nobody mentioned.
 *
 * The two flows are safe to collect from anywhere.
 */
interface PlaybackCoordinator {
    val playback: Flow<PlaybackSummary>

    /** Milliseconds left on the sleep timer, or null when no timer is running. */
    val sleepTimerRemainingMs: Flow<Long?>

    /**
     * Wants [itemId] playing: resumes it when the session is already on it, and otherwise makes it
     * the session's item and starts it.
     *
     * Takes an id rather than the thing itself, so the metadata the media session publishes is read
     * beside the source it is paired with. A screen handing over its own `Music` could pair a stale
     * title with a fresh URI, and nothing would have noticed.
     *
     * The session is on [itemId] from the moment this is called — before the source lookup that
     * follows — so a second tap finds something to pause rather than an idle session to start again.
     *
     * An item of a kind the session has no resolver for fails as
     * [PlaybackFailure.ItemNotFound]; it never reaches the engine.
     */
    suspend fun play(itemId: PlaybackItemId)

    /**
     * Stops wanting playback, and abandons a source lookup still in flight.
     *
     * Paired with [play] rather than folded into one `toggle`, because the choice between them
     * belongs to whatever the listener is looking at: a screen branches on
     * [PlaybackSummary.playIntent], which is the same value its play/pause control renders. A
     * toggle inside the session decided from a value the screen never saw.
     */
    fun pause()

    fun setLooping(enabled: Boolean)
    fun setVolume(volume: Float)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
}
