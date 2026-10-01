package com.xwab.app.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

/** Which duration the wheel opens on, so Restart repeats the running timer rather than the default. */
class SleepTimerPresetTest {
    @Test
    fun withNoTimerTheWheelOpensOnThirtyMinutes() {
        assertEquals(1, initialPresetIndex(null))
    }

    @Test
    fun aRunningTimerOpensTheWheelOnTheShortestPresetThatCoversWhatIsLeft() {
        assertEquals(0, initialPresetIndex(14L * MINUTE))
        assertEquals(0, initialPresetIndex(15L * MINUTE))
        assertEquals(1, initialPresetIndex(29L * MINUTE + 20_000L))
        assertEquals(2, initialPresetIndex(58L * MINUTE))
        assertEquals(3, initialPresetIndex(89L * MINUTE))
    }

    @Test
    fun moreThanTheLongestPresetLeftOpensOnTheLongest() {
        assertEquals(3, initialPresetIndex(120L * MINUTE))
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
