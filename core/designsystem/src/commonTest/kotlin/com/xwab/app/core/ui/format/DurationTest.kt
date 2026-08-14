package com.xwab.app.core.ui.format

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
