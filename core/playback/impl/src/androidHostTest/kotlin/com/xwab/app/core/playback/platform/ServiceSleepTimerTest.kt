package com.xwab.app.core.playback.platform

import com.xwab.app.core.playback.timer.SLEEP_TIMER_FADE_MS
import com.xwab.app.core.playback.timer.SLEEP_TIMER_FADE_STEP_MS
import com.xwab.app.core.playback.timer.SleepTimerClock
import com.xwab.app.core.playback.timer.TickScheduler
import com.xwab.app.core.playback.timer.sleepTimerFadeVolume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The service-owned timer, driven through the clock and scheduler its graph gives it. The service
 * device test covers the IPC around it; this covers what it does with the player.
 */
class ServiceSleepTimerTest {

    private class FakeScheduler : TickScheduler {
        var pending: (() -> Unit)? = null
        var lastDelayMs: Long? = null

        override fun schedule(delayMs: Long, action: () -> Unit) {
            pending = action
            lastDelayMs = delayMs
        }

        override fun cancel() {
            pending = null
        }

        /** Advance the clock by the scheduled delay and run the tick, as the main looper would. */
        fun runPendingTick(advance: (Long) -> Unit) {
            val tick = requireNotNull(pending) { "No tick scheduled." }
            advance(requireNotNull(lastDelayMs))
            pending = null
            tick()
        }
    }

    private var now = START_MS
    private val scheduler = FakeScheduler()
    /** What the player was told, in order. */
    private val events = mutableListOf<String>()
    private val timer = SleepTimer(
        clock = SleepTimerClock { now },
        scheduler = scheduler,
        onExpired = { events += EXPIRED },
        onFadeVolume = { events += it.toString() },
    )

    @Test
    fun aDeadlineThatHasPassedIsRefusedAndChangesNothing() {
        assertFalse(timer.startUntil(now))

        assertNull(timer.deadlineElapsedRealtimeMs)
        assertNull(scheduler.pending)
        assertTrue(events.isEmpty(), "a refused deadline must not touch the player: $events")
    }

    @Test
    fun beforeTheFadeWindowTheTimerSleepsUntilItOpens() {
        assertTrue(timer.startUntil(now + SLEEP_TIMER_FADE_MS + LEAD_MS))

        assertEquals(LEAD_MS, scheduler.lastDelayMs)
        assertTrue(events.isEmpty(), "full volume must be left alone: $events")
    }

    @Test
    fun theFadeStepsDownAndPlaybackStopsBeforeFullVolumeReturns() {
        timer.startUntil(now + SLEEP_TIMER_FADE_MS)
        assertEquals(SLEEP_TIMER_FADE_STEP_MS, scheduler.lastDelayMs)

        scheduler.runPendingTick { now += it }
        assertEquals(
            sleepTimerFadeVolume(SLEEP_TIMER_FADE_MS - SLEEP_TIMER_FADE_STEP_MS).toString(),
            events.last(),
        )

        while (scheduler.pending != null) scheduler.runPendingTick { now += it }

        assertEquals(listOf(EXPIRED, FULL_VOLUME.toString()), events.takeLast(2))
        assertNull(timer.deadlineElapsedRealtimeMs)
    }

    @Test
    fun cancellingMidFadeRestoresFullVolumeAndDropsTheTick() {
        timer.startUntil(now + SLEEP_TIMER_FADE_MS)
        scheduler.runPendingTick { now += it }

        timer.cancel()

        assertEquals(FULL_VOLUME.toString(), events.last())
        assertNull(scheduler.pending)
        assertNull(timer.deadlineElapsedRealtimeMs)
        assertFalse(EXPIRED in events)
    }

    @Test
    fun restartingMidFadeRestoresFullVolumeForTheNewDeadline() {
        timer.startUntil(now + SLEEP_TIMER_FADE_MS)
        scheduler.runPendingTick { now += it }
        val later = now + SLEEP_TIMER_FADE_MS + LEAD_MS

        assertTrue(timer.startUntil(later))

        assertEquals(FULL_VOLUME.toString(), events.last())
        assertEquals(later, timer.deadlineElapsedRealtimeMs)
        assertEquals(LEAD_MS, scheduler.lastDelayMs)
    }

    private companion object {
        const val START_MS = 1_000L
        const val LEAD_MS = 5_000L
        const val FULL_VOLUME = 1.0f
        const val EXPIRED = "expired"
    }
}
