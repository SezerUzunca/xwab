package com.xwab.app.core.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrackIdTest {
    @Test
    fun theIdIsTheStringItWasGiven() {
        assertEquals("gentle-rain", TrackId("gentle-rain").value)
        assertEquals("gentle-rain", TrackId("gentle-rain").toString())
    }

    /**
     * A blank id names a track no lookup can match, so it is refused where it is built.
     *
     * The one id that arrives from outside the catalog is the stored favorites set, and that reader
     * drops blanks before they get here. A navigation route also carries one, but it is only ever
     * written from a catalog id, so it cannot be blank without the route itself being corrupt.
     */
    @Test
    fun aBlankIdIsRejected() {
        assertFailsWith<IllegalArgumentException> { TrackId("") }
        assertFailsWith<IllegalArgumentException> { TrackId("   ") }
    }
}
