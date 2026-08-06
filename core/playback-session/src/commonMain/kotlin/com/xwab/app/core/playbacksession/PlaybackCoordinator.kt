package com.xwab.app.core.playbacksession

import com.xwab.app.core.catalog.TrackId
import kotlinx.coroutines.flow.Flow

/**
 * Control over the single playback session the app runs.
 *
 * Not a repository: there is nothing to store or query here, only a live session to steer.
 *
 * Everything it publishes is playback-capability-owned. The engine's own state model stays
 * behind the adapter, which keeps engine details away from features.
 */
interface PlaybackCoordinator {
    val playback: Flow<PlaybackSummary>

    /** Milliseconds left on the sleep timer, or null when no timer is running. */
    val sleepTimerRemainingMs: Flow<Long?>

    /**
     * Wants [trackId] playing: resumes it when the session is already on it, and otherwise makes it
     * the session's track and starts it.
     *
     * Takes an id rather than a track, so the metadata the media session publishes is read from the
     * catalog beside the source it is paired with. A screen handing over its own `Music` could pair
     * a stale title with a fresh URI, and nothing would have noticed.
     *
     * The session is on [trackId] from the moment this is called — before the source lookup that
     * follows — so a second tap finds something to pause rather than an idle session to start again.
     */
    suspend fun play(trackId: TrackId)

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
