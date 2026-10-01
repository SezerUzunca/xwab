package com.xwab.app.designsystem.format

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Moved here with the formatter it covers. The seconds field is the part that used to be written
 * three different ways, so the padding cases are the ones worth keeping.
 */
class DurationTest {
    @Test
    fun secondsBelowTenArePadded() {
        assertEquals("0:09", formatDuration(9))
        assertEquals("1:05", formatDuration(65))
    }

    @Test
    fun wholeMinutesShowTwoZeroes() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("2:00", formatDuration(120))
    }

    @Test
    fun longRunningTimesKeepCountingInMinutes() {
        assertEquals("90:00", formatDuration(5_400))
    }

    /** Both catalogs refuse a negative duration, so this is a last resort rather than a contract. */
    @Test
    fun aNegativeTotalIsClampedRatherThanFormattedAsNegative() {
        assertEquals("0:00", formatDuration(-1))
    }
}

/**
 * The rounding is the whole point: it is the difference between a timer that reads "1 min" for its
 * last moments and one that sits on "0 min" while sound still plays.
 */
class RemainingTest {
    @Test
    fun aPartMinuteStillCountsAsAMinute() {
        assertEquals(1L, remainingWholeMinutes(1))
        assertEquals(1L, remainingWholeMinutes(20_000))
        assertEquals(1L, remainingWholeMinutes(60_000))
        assertEquals(2L, remainingWholeMinutes(60_001))
    }

    @Test
    fun onlyAnExhaustedTimerReadsZero() {
        assertEquals(0L, remainingWholeMinutes(0))
    }

    @Test
    fun aFullPresetReadsAsItsOwnLength() {
        assertEquals(15L, remainingWholeMinutes(15L * 60_000L))
        assertEquals(90L, remainingWholeMinutes(90L * 60_000L))
    }

    /** Nothing hands this a negative, but a clock that went backwards should not print one. */
    @Test
    fun aNegativeRemainderIsClampedRatherThanShownAsNegative() {
        assertEquals(0L, remainingWholeMinutes(-1))
    }
}
