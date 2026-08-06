package com.xwab.app.core.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatDurationTest {
    @Test
    fun formatDurationIsStable() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("4:47", formatDuration(287))
    }
}
