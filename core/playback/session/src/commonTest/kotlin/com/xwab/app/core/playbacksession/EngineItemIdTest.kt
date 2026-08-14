package com.xwab.app.core.playbacksession

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The encoding that crosses a process boundary: on Android the media service holds these ids and
 * outlives the app, so what is written here is read back by a build that may not be this one.
 */
class EngineItemIdTest {
    @Test
    fun anItemSurvivesTheTripThroughTheEngine() {
        listOf(PlaybackItemId.sound("gentle-rain"), PlaybackItemId.story("night-came-slowly"))
            .forEach { item ->
                assertEquals(item, playbackItemIdOf(item.toEngineId()))
            }
    }

    @Test
    fun theKindIsPartOfTheEngineId() {
        assertEquals("sound:gentle-rain", PlaybackItemId.sound("gentle-rain").toEngineId())
        assertEquals("story:gentle-rain", PlaybackItemId.story("gentle-rain").toEngineId())
    }

    /** Same raw id, two items, two sources — which is the whole reason for the prefix. */
    @Test
    fun aSoundAndAStoryWithOneRawIdDoNotCollide() {
        assertEquals(
            false,
            PlaybackItemId.sound("forest").toEngineId() == PlaybackItemId.story("forest").toEngineId(),
        )
    }

    /**
     * A service started by an earlier build holds bare track ids. Reading one as a sound is what
     * keeps a reconnect attached to what is already playing.
     */
    @Test
    fun anIdWrittenBeforeNamespacingIsReadAsASound() {
        assertEquals(PlaybackItemId.sound("gentle-rain"), playbackItemIdOf("gentle-rain"))
    }

    /** Nothing else in the app writes these, but the engine's id is a plain string all the same. */
    @Test
    fun anIdThatNamesNothingIsNull() {
        assertNull(playbackItemIdOf(""))
        assertNull(playbackItemIdOf("   "))
        assertNull(playbackItemIdOf("sound:"))
        assertNull(playbackItemIdOf("story:"))
    }

    /** An unknown prefix is not a kind, so the whole string is the sound's id — colon and all. */
    @Test
    fun anUnknownPrefixIsNotTreatedAsAKind() {
        assertEquals(PlaybackItemId.sound("podcast:daily"), playbackItemIdOf("podcast:daily"))
    }
}
