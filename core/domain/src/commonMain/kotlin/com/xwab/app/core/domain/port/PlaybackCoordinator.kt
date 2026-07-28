package com.xwab.app.core.domain.port

import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow

/**
 * Control over the single playback session the app runs.
 *
 * Not a repository: there is nothing to store or query here, only a live session to steer.
 *
 * Everything it publishes is domain-owned. The engine's own state model stays behind the
 * adapter, which is what keeps `core:media` off the upper layers' classpath.
 */
interface PlaybackCoordinator {
    val playback: Flow<PlaybackSummary>

    /** Milliseconds left on the sleep timer, or null when no timer is running. */
    val sleepTimerRemainingMs: Flow<Long?>

    fun togglePlayback(music: Music)
    fun setLooping(enabled: Boolean)
    fun setVolume(volume: Float)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
}
