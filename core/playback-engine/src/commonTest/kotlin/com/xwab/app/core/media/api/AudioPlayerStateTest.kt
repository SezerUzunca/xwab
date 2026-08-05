package com.xwab.app.core.media.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioPlayerStateTest {
    @Test
    fun sourceRequiresStableIdentityAndUri() {
        assertFailsWith<IllegalArgumentException> { AudioSource(id = "", uri = "file.mp3") }
        assertFailsWith<IllegalArgumentException> { AudioSource(id = "rain", uri = "") }
    }

    @Test
    fun playbackRequestValidatesVolumeBounds() {
        val source = AudioSource(id = "ocean", uri = "ocean.mp3")
        assertFailsWith<IllegalArgumentException> {
            PlaybackRequest(source = source, volume = -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            PlaybackRequest(source = source, volume = 1.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            PlaybackRequest(source = source, volume = Float.NaN)
        }
        val validRequest = PlaybackRequest(source = source, volume = 0.5f, loopMode = LoopMode.One)
        assertEquals(0.5f, validRequest.volume)
        assertEquals(LoopMode.One, validRequest.loopMode)
    }

    @Test
    fun sleepTimerStateValidatesRemainingTime() {
        assertFailsWith<IllegalArgumentException> { SleepTimerState(remainingMs = -100L) }

        val inactiveTimer = SleepTimerState()
        assertNull(inactiveTimer.remainingMs)

        val expiredTimer = SleepTimerState(remainingMs = 0L)
        assertEquals(0L, expiredTimer.remainingMs)

        val activeTimer = SleepTimerState(remainingMs = 5000L)
        assertEquals(5000L, activeTimer.remainingMs)
    }

    @Test
    fun initialStateIsIdle() {
        val state = AudioPlayerState()

        assertEquals(PlaybackPhase.Idle, state.phase)
        assertFalse(state.playRequested)
        assertFalse(state.isPlaying)
        assertNull(state.source)
        assertNull(state.requestedSource)
        assertNull(state.error)
    }

    @Test
    fun desiredPlaybackIsIndependentFromActualPlayback() {
        val state = AudioPlayerState(
            playRequested = true,
            isPlaying = false,
            phase = PlaybackPhase.Buffering,
        )

        assertTrue(state.playRequested)
        assertFalse(state.isPlaying)
    }

    @Test
    fun activeSourceFallsBackToRequestedSourceDuringReconnect() {
        val requested = AudioSource(id = "rain", uri = "file.mp3")
        val state = AudioPlayerState(requestedSource = requested, phase = PlaybackPhase.Loading)

        assertEquals(requested, state.activeSource)
        assertTrue(state.source == null)
    }

    @Test
    fun playbackErrorStoresCodeAndMessage() {
        val error = PlaybackError(code = PlaybackErrorCode.InvalidSource, message = "Invalid URI")
        assertEquals(PlaybackErrorCode.InvalidSource, error.code)
        assertEquals("Invalid URI", error.message)
    }
}
