package com.xwab.app.core.playback.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
}
