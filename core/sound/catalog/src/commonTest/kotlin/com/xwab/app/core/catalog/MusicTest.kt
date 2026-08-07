package com.xwab.app.core.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The same invariants the story catalog's model holds, checked the same way. The manifest is
 * hand-written today and may be fed later; what is refused here is what would otherwise reach a
 * screen as a blank row or an impossible duration.
 */
class MusicTest {
    @Test
    fun aTrackKeepsTheMetadataItWasGiven() {
        val track = track()

        assertEquals(TrackId("gentle-rain"), track.id)
        assertEquals("Rain on the Window", track.name)
        assertEquals("rain", track.categoryId)
        assertEquals("0:09", track.formattedDuration)
    }

    /** The playback pair falls back to the track's own name and the app's own artist. */
    @Test
    fun thePlaybackMetadataDefaultsToTheTrackItself() {
        val track = track()

        assertEquals("Rain on the Window", track.playbackTitle)
        assertEquals("Sleep Sounds", track.playbackArtist)
    }

    @Test
    fun aTrackWithoutANameIsRejected() {
        assertFailsWith<IllegalArgumentException> { track(name = "") }
        assertFailsWith<IllegalArgumentException> { track(name = "  ") }
    }

    @Test
    fun aTrackWithoutACategoryIsRejected() {
        assertFailsWith<IllegalArgumentException> { track(categoryId = "") }
    }

    @Test
    fun aDurationThatIsNotPositiveIsRejected() {
        assertFailsWith<IllegalArgumentException> { track(durationSeconds = 0) }
        assertFailsWith<IllegalArgumentException> { track(durationSeconds = -1) }
    }

    /** These two are published to the platform media session, where a blank shows as a blank. */
    @Test
    fun blankPlaybackMetadataIsRejected() {
        assertFailsWith<IllegalArgumentException> { track(playbackTitle = " ") }
        assertFailsWith<IllegalArgumentException> { track(playbackArtist = "") }
    }

    private fun track(
        name: String = "Rain on the Window",
        categoryId: String = "rain",
        durationSeconds: Int = 9,
        playbackTitle: String = name,
        playbackArtist: String = "Sleep Sounds",
    ) = Music(
        id = TrackId("gentle-rain"),
        name = name,
        categoryId = categoryId,
        durationSeconds = durationSeconds,
        playbackTitle = playbackTitle,
        playbackArtist = playbackArtist,
    )
}
