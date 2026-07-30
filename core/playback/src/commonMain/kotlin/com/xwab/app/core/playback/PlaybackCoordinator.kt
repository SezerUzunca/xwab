package com.xwab.app.core.playback

import com.xwab.app.core.model.Music
import com.xwab.app.core.model.PlaybackSummary
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

    suspend fun togglePlayback(music: Music)
    fun setLooping(enabled: Boolean)
    fun setVolume(volume: Float)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
}
