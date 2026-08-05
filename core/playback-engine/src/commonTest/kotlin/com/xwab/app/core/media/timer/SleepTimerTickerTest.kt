package com.xwab.app.core.media.timer

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
    private val ticker = SleepTimerTicker(
        nowMs = { now },
        scheduler = scheduler,
        onExpired = { expiredCalls++ },
    )

    @Test
    fun applyingDeadlinePublishesRemainingTimeAndSchedulesTick() {
        ticker.applyDeadline(now + 5_000L)

        assertEquals(5_000L, ticker.state.value.remainingMs)
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
    fun lastTickBeforeDeadlineIsShorterThanTheUpdateInterval() {
        ticker.applyDeadline(now + 400L)

        assertEquals(400L, scheduler.lastDelayMs)
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
}
