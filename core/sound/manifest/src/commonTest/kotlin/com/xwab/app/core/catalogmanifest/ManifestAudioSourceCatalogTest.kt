package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The port `core:sound:delivery` reads the manifest through.
 *
 * What it must never do is answer for a track the catalog does not hold: the resolver takes that
 * answer as permission to fetch, so a stray source would put the cache to work on a URL nothing in
 * the app refers to — and the sweep, reading the same port, would then delete the result.
 */
class ManifestAudioSourceCatalogTest {
    private val catalog = ManifestAudioSourceCatalog(
        listOf(
            CatalogEntry(
                Music(TrackId("heavy-rain"), "Heavy Rain", CategoryId("rain"), 30),
                "https://example.test/heavy-rain.mp3",
            ),
            CatalogEntry(
                Music(TrackId("calm-waves"), "Calm Waves", CategoryId("ocean"), 40),
                "https://example.test/calm-waves.mp3",
                version = 2,
            ),
        ),
    )

    @Test
    fun aTrackIsAnsweredWithItsCacheNameAndItsSource() {
        val source = assertNotNull(catalog.sourceFor(TrackId("heavy-rain")))

        assertEquals("heavy-rain-v1.mp3", source.cacheFileName)
        assertEquals("https://example.test/heavy-rain.mp3", source.httpsUrl)
    }

    @Test
    fun anUnknownTrackHasNoSource() {
        assertNull(catalog.sourceFor(TrackId("no-such-track")))
    }

    @Test
    fun theNamesToKeepAreExactlyWhatTheEntriesReferTo() {
        assertEquals(setOf("heavy-rain-v1.mp3", "calm-waves-v2.mp3"), catalog.cacheFileNames)
    }

    /**
     * The shipped manifest, since that is the one the sweep is measured against on a device: a
     * track missing from this port would have its cached file deleted after the next download.
     */
    @Test
    fun everyShippedTrackIsReachableThroughThePort() {
        val shipped = ManifestAudioSourceCatalog()

        assertEquals(catalogEntries.size, shipped.cacheFileNames.size)
        catalogEntries.forEach { entry ->
            assertEquals(entry.cacheFileName, shipped.sourceFor(entry.music.id)?.cacheFileName)
        }
    }
}
