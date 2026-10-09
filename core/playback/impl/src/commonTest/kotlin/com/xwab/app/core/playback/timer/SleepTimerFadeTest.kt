package com.xwab.app.core.playback.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepTimerFadeTest {
    @Test
    fun fullVolumeUntilTheFadeWindowAndSilenceAtTheDeadline() {
        assertEquals(1.0f, sleepTimerFadeVolume(10 * 60_000L))
        assertEquals(1.0f, sleepTimerFadeVolume(SLEEP_TIMER_FADE_MS))
        assertEquals(0.0f, sleepTimerFadeVolume(0L))
    }

    /** Squared, so the audible change is spread across the window rather than bunched at the end. */
    @Test
    fun halfwayThroughTheFadeIsAQuarterOfTheGain() {
        assertEquals(0.25f, sleepTimerFadeVolume(SLEEP_TIMER_FADE_MS / 2))
    }

    @Test
    fun theVolumeOnlyFallsAsTheDeadlineNears() {
        val volumes = (SLEEP_TIMER_FADE_MS downTo 0L step SLEEP_TIMER_FADE_STEP_MS).map(::sleepTimerFadeVolume)

        assertTrue(volumes.zipWithNext().all { (earlier, later) -> later < earlier })
    }

    @Test
    fun ticksOnceASecondUntilTheFadeAndEveryStepWithinIt() {
        assertEquals(1_000L, sleepTimerTickDelay(10 * 60_000L, secondMs = 1_000L))
        assertEquals(SLEEP_TIMER_FADE_STEP_MS, sleepTimerTickDelay(SLEEP_TIMER_FADE_MS, secondMs = 1_000L))
        assertEquals(100L, sleepTimerTickDelay(100L, secondMs = 1_000L))
    }
}
