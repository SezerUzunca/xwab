package com.xwab.app.core.session

import com.xwab.app.core.session.port.PlaybackItemId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * The encoding that crosses a process boundary: on Android the media service holds these ids and
 * outlives the app, so what is written here is read back by a build that may not be this one.
 *
 * The kinds here are this test's own strings, not constants borrowed from a content module. That is
 * the property under test: this module encodes whatever kind it is handed and has no list of them.
 */
class EngineItemIdTest {

    @Test
    fun anItemSurvivesTheTripThroughTheEngine() {
        listOf(
            PlaybackItemId("sound", "gentle-rain"),
            PlaybackItemId("story", "night-came-slowly"),
            // A kind no module in this build registers still round-trips: encoding is not the
            // place that decides whether anything can play it.
            PlaybackItemId("meditation", "body-scan"),
        ).forEach { item ->
            assertEquals(item, playbackItemIdOf(item.toEngineId()))
        }
    }

    @Test
    fun theKindIsPartOfTheEngineId() {
        assertEquals("sound:gentle-rain", PlaybackItemId("sound", "gentle-rain").toEngineId())
        assertEquals("story:gentle-rain", PlaybackItemId("story", "gentle-rain").toEngineId())
    }

    /** Same raw id, two items, two sources — which is the whole reason for the prefix. */
    @Test
    fun twoKindsWithOneRawIdDoNotCollide() {
        assertNotEquals(
            PlaybackItemId("sound", "forest").toEngineId(),
            PlaybackItemId("story", "forest").toEngineId(),
        )
    }

    /**
     * A prefix this build has no content module for is still read as the kind it claims to be.
     *
     * This is what a removed content type looks like from here, and reading it honestly is what
     * lets the session answer `ItemNotFound` rather than resolving it as something else.
     */
    @Test
    fun aKindNothingRegistersIsStillReadAsThatKind() {
        assertEquals(PlaybackItemId("podcast", "daily"), playbackItemIdOf("podcast:daily"))
    }

    /**
     * An id with no separator names no kind, and this module has no default to fall back on.
     *
     * It used to answer `sound` here — an upgrade path for a service still running from a build
     * that wrote bare track ids. That could not survive kinds becoming open: nothing in this module
     * knows which kind would be the one to guess. No released build ever wrote a bare id, so a
     * service holding one now reports nothing attached rather than the wrong thing.
     */
    @Test
    fun anIdWithNoKindNamesNothing() {
        assertNull(playbackItemIdOf("gentle-rain"))
    }

    /** Nothing else in the app writes these, but the engine's id is a plain string all the same. */
    @Test
    fun anIdThatNamesNothingIsNull() {
        assertNull(playbackItemIdOf(""))
        assertNull(playbackItemIdOf("   "))
        assertNull(playbackItemIdOf("sound:"))
        assertNull(playbackItemIdOf("story:"))
        assertNull(playbackItemIdOf(":gentle-rain"))
    }
}
