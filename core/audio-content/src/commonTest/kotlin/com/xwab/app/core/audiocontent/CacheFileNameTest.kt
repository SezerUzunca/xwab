package com.xwab.app.core.audiocontent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Both file stores delete whatever this returns, so a false positive costs a listener the audio
 * they just downloaded.
 */
class CacheFileNameTest {
    @Test
    fun olderVersionsOfTheSameTrackAreSuperseded() {
        val existing = listOf("heavy-rain-v1.mp3", "heavy-rain-v2.mp3", "heavy-rain-v3.mp3")

        assertEquals(
            listOf("heavy-rain-v1.mp3", "heavy-rain-v2.mp3"),
            supersededCacheFileNames(existing, current = "heavy-rain-v3.mp3"),
        )
    }

    @Test
    fun otherTracksAreNeverSuperseded() {
        val existing = listOf("heavy-rain-window-v1.mp3", "rain-v1.mp3", "window-storm-v1.mp3")

        assertTrue(supersededCacheFileNames(existing, current = "heavy-rain-v2.mp3").isEmpty())
    }

    /**
     * A track id may contain the letter sequence the version marker uses, so the split has to take
     * the last one.
     */
    @Test
    fun anIdContainingTheVersionMarkerStillMatchesItsOwnVersionsOnly() {
        val existing = listOf("night-view-v1.mp3", "night-v1.mp3")

        assertEquals(
            listOf("night-view-v1.mp3"),
            supersededCacheFileNames(existing, current = "night-view-v2.mp3"),
        )
    }

    @Test
    fun unrelatedOrMalformedNamesAreLeftAlone() {
        val existing = listOf(
            ".heavy-rain-v1.mp3.part",
            "heavy-rain-v1.mp3.tmp",
            "heavy-rain-v1.wav",
            "Heavy-Rain-v1.mp3",
        )

        assertTrue(supersededCacheFileNames(existing, current = "heavy-rain-v2.mp3").isEmpty())
    }

    @Test
    fun aNameWithoutAVersionSupersedesNothing() {
        assertTrue(supersededCacheFileNames(listOf("heavy-rain.mp3"), current = "heavy-rain.mp3").isEmpty())
    }
}
