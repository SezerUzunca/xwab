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
    fun olderVersionsOfATrackAreUnreferenced() {
        val existing = listOf("heavy-rain-v1.mp3", "heavy-rain-v2.mp3", "heavy-rain-v3.mp3")

        assertEquals(
            listOf("heavy-rain-v1.mp3", "heavy-rain-v2.mp3"),
            unreferencedCacheFileNames(existing, keep = setOf("heavy-rain-v3.mp3")),
        )
    }

    /** A track dropped from the manifest leaves a file that no version bump would ever clear. */
    @Test
    fun aTrackThatLeftTheCatalogIsUnreferenced() {
        val existing = listOf("heavy-rain-v1.mp3", "window-storm-v1.mp3")

        assertEquals(
            listOf("window-storm-v1.mp3"),
            unreferencedCacheFileNames(existing, keep = setOf("heavy-rain-v1.mp3")),
        )
    }

    @Test
    fun referencedFilesAreKeptWhicheverTrackTheyBelongTo() {
        val existing = listOf("heavy-rain-v1.mp3", "rain-v1.mp3", "night-view-v2.mp3")

        assertTrue(unreferencedCacheFileNames(existing, keep = existing.toSet()).isEmpty())
    }

    /**
     * A transfer in progress is staged under a name this must not match, or a second track
     * finishing first would delete the file the first one is still writing.
     */
    @Test
    fun partialAndMalformedNamesAreLeftAlone() {
        val existing = listOf(
            partialCacheFileName("heavy-rain-v1.mp3"),
            "heavy-rain-v1.mp3.tmp",
            "heavy-rain-v1.wav",
            "Heavy-Rain-v1.mp3",
            "heavy-rain.mp3",
        )

        assertTrue(unreferencedCacheFileNames(existing, keep = emptySet()).isEmpty())
    }

    /** The manifest is the keep list, so a name it omits is a name the cache must not hold. */
    @Test
    fun everyCatalogCacheFileNameIsWellFormedAndKept() {
        assertTrue(catalogCacheFileNames.all { CACHE_FILE_NAME.matches(it) })
        assertTrue(unreferencedCacheFileNames(catalogCacheFileNames.toList(), catalogCacheFileNames).isEmpty())
    }
}
