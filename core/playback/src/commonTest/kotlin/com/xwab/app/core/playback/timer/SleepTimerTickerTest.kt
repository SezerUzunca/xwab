package com.xwab.app.core.playback.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the shared sleep-timer countdown, using a fake clock and scheduler so
 * the deadline behavior is deterministic instead of wall-clock dependent.
 */
class SleepTimerTickerTest {

    /** Records the pending tick instead of posting it to a real main thread. */
    private class FakeScheduler : TickScheduler {
        var pending: (() -> Unit)? = null
        var lastDelayMs: Long? = null
        var releaseCalls = 0

        override fun schedule(delayMs: Long, action: () -> Unit) {
            lastDelayMs = delayMs
            pending = action
        }

        override fun cancel() {
            pending = null
        }

        override fun release() {
            releaseCalls++
            cancel()
        }

        /** Run the scheduled tick, as the platform scheduler eventually would. */
        fun runPendingTick() {
            val tick = requireNotNull(pending) { "No tick scheduled." }
            pending = null
            tick()
        }
    }

    private var now = 1_000L
    private val scheduler = FakeScheduler()
    private var expiredCalls = 0
    /** What the player was told, in order, with expiry recorded where it happened. */
    private val events = mutableListOf<String>()
    private val volumes get() = events.filter { it != EXPIRED }.map(String::toFloat)
    private val ticker = SleepTimerTicker(
        clock = { now },
        scheduler = scheduler,
        onExpired = {
            expiredCalls++
            events += EXPIRED
        },
        onFadeVolume = { events += it.toString() },
    )

    @Test
    fun theVolumeIsLeftAloneUntilTheFadeWindowOpens() {
        ticker.applyDeadline(now + SLEEP_TIMER_FADE_MS + 5_000L)

        assertTrue(events.isEmpty(), "a timer far from its end must not touch the volume: $events")
        assertEquals(1_000L, scheduler.lastDelayMs)
    }

    /** A sound that stops dead can wake the sleeper; the last half minute lowers it step by step. */
    @Test
    fun theLastHalfMinuteFadesStepByStepAndPlaybackStopsBeforeFullVolumeReturns() {
        ticker.applyDeadline(now + SLEEP_TIMER_FADE_MS)
        assertEquals(SLEEP_TIMER_FADE_STEP_MS, scheduler.lastDelayMs)

        while (scheduler.pending != null) {
            now += SLEEP_TIMER_FADE_STEP_MS
            scheduler.runPendingTick()
        }

        val beforeExpiry = volumes.dropLast(1)
        assertTrue(beforeExpiry.size > 100, "fades in many small steps, got ${beforeExpiry.size}")
        assertTrue(beforeExpiry.zipWithNext().all { (a, b) -> b < a }, "only ever falls: $beforeExpiry")
        assertEquals(1, expiredCalls)
        // Stopped first, then full volume for the next play — never full volume while still audible.
        assertEquals(listOf(EXPIRED, "1.0"), events.takeLast(2))
    }

    @Test
    fun cancellingMidFadeRestoresFullVolume() {
        ticker.applyDeadline(now + SLEEP_TIMER_FADE_MS / 2)

        ticker.clear()

        assertEquals(1.0f, volumes.last())
        assertEquals(0, expiredCalls)
    }

    @Test
    fun restartingALongerTimerMidFadeRestoresFullVolume() {
        ticker.applyDeadline(now + SLEEP_TIMER_FADE_MS / 2)

        ticker.applyDeadline(now + 15 * 60_000L)

        assertEquals(1.0f, volumes.last())
        assertEquals(1_000L, scheduler.lastDelayMs)
    }

    @Test
    fun applyingDeadlinePublishesRemainingTimeAndSchedulesTick() {
        ticker.applyDeadline(now + 60_000L)

        assertEquals(60_000L, ticker.state.value.remainingMs)
        // Re-arms once per second rather than sleeping until the deadline.
        assertEquals(1_000L, scheduler.lastDelayMs)
    }

    @Test
    fun remainingTimeFollowsTheClockNotTheTickCount() {
        ticker.applyDeadline(now + 5_000L)

        // A delayed tick (e.g. in the background) must not stretch the countdown.
        now += 3_000L
        scheduler.runPendingTick()

        assertEquals(2_000L, ticker.state.value.remainingMs)
    }

    @Test
    fun reachingDeadlineClearsStateAndReportsExpiryOnce() {
        ticker.applyDeadline(now + 1_000L)
        now += 1_000L

        scheduler.runPendingTick()

        assertEquals(1, expiredCalls)
        assertNull(ticker.state.value.remainingMs)
        assertNull(scheduler.pending)
    }

    @Test
    fun clearStopsCountdownWithoutReportingExpiry() {
        ticker.applyDeadline(now + 5_000L)

        ticker.clear()

        assertEquals(0, expiredCalls)
        assertNull(ticker.state.value.remainingMs)
        assertNull(scheduler.pending)
    }

    @Test
    fun lastTickBeforeDeadlineIsShorterThanAFadeStep() {
        ticker.applyDeadline(now + 100L)

        assertEquals(100L, scheduler.lastDelayMs)
    }

    @Test
    fun applyingAPastDeadlineExpiresImmediately() {
        ticker.applyDeadline(now - 1L)

        assertEquals(1, expiredCalls)
        assertNull(ticker.state.value.remainingMs)
    }

    @Test
    fun minimumLongDeadlineCannotOverflowIntoAnActiveTimer() {
        ticker.applyDeadline(Long.MIN_VALUE)

        assertEquals(1, expiredCalls)
        assertNull(ticker.state.value.remainingMs)
        assertNull(scheduler.pending)
    }

    @Test
    fun releaseTearsDownTheSchedulerAndStopsTicking() {
        ticker.applyDeadline(now + 5_000L)

        ticker.release()

        assertTrue(scheduler.releaseCalls > 0)
        assertNull(scheduler.pending)
        assertNull(ticker.state.value.remainingMs)
    }

    private companion object {
        const val EXPIRED = "expired"
    }
}
