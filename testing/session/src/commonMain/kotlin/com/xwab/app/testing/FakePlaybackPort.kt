package com.xwab.app.testing

import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An in-memory [PlaybackPort] that records what a screen asked of it and publishes whatever summary
 * a test hands it.
 *
 * Content-neutral like the session itself: it plays a [PlaybackItemId] of any kind, so the story
 * list and the now-playing bar can declare it without a sound catalog on their test classpath.
 */
class FakePlaybackPort : PlaybackPort {
    private val summary = MutableStateFlow(PlaybackSummary())
    private val remainingMs = MutableStateFlow<Long?>(null)

    override val playback: Flow<PlaybackSummary> = summary
    override val sleepTimerRemainingMs: Flow<Long?> = remainingMs

    var playedItemId: PlaybackItemId? = null
    var pauses = 0
    var looping: Boolean? = null
    var volume: Float? = null
    var startedTimerMs: Long? = null
    var cancelledTimers = 0

    fun publish(playback: PlaybackSummary) {
        summary.value = playback
    }

    fun publishSleepTimer(remaining: Long?) {
        remainingMs.value = remaining
    }

    override suspend fun play(itemId: PlaybackItemId) {
        playedItemId = itemId
    }

    override fun pause() {
        pauses++
    }

    override fun setLooping(enabled: Boolean) {
        looping = enabled
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
    }

    override fun startSleepTimer(durationMs: Long) {
        startedTimerMs = durationMs
    }

    override fun cancelSleepTimer() {
        cancelledTimers++
    }
}
