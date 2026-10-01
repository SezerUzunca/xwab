package com.xwab.app.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

/** Which preset is shown selected for a timer that was already running when the screen opened. */
class SleepTimerPresetTest {
    @Test
    fun aRunningTimerSelectsTheShortestPresetThatCoversWhatIsLeft() {
        assertEquals(15, presetMinutesFor(14L * MINUTE))
        assertEquals(15, presetMinutesFor(15L * MINUTE))
        assertEquals(30, presetMinutesFor(29L * MINUTE + 20_000L))
        assertEquals(60, presetMinutesFor(58L * MINUTE))
        assertEquals(90, presetMinutesFor(89L * MINUTE))
    }

    @Test
    fun moreThanTheLongestPresetLeftSelectsTheLongest() {
        assertEquals(90, presetMinutesFor(120L * MINUTE))
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
