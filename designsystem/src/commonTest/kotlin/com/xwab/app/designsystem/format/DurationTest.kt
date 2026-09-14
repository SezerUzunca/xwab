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
 * The rounding is the whole point of this one existing separately: it is the difference between a
 * timer that reads `0:01` for its last moment and one that sits on `0:00` while sound still plays.
 */
class RemainingTest {
    @Test
    fun aPartSecondStillCountsAsASecond() {
        assertEquals("0:01", formatRemaining(1))
        assertEquals("0:01", formatRemaining(200))
        assertEquals("0:01", formatRemaining(1_000))
    }

    @Test
    fun onlyAnExhaustedTimerReadsZero() {
        assertEquals("0:00", formatRemaining(0))
    }

    @Test
    fun secondsBelowTenArePadded() {
        assertEquals("1:05", formatRemaining(65_000))
        assertEquals("0:09", formatRemaining(9_000))
    }

    @Test
    fun aFullPresetReadsAsItsOwnLength() {
        assertEquals("15:00", formatRemaining(15L * 60_000L))
        assertEquals("60:00", formatRemaining(60L * 60_000L))
    }

    /** Nothing hands this a negative, but a clock that went backwards should not print one. */
    @Test
    fun aNegativeRemainderIsClampedRatherThanFormattedAsNegative() {
        assertEquals("0:00", formatRemaining(-1))
    }
}
