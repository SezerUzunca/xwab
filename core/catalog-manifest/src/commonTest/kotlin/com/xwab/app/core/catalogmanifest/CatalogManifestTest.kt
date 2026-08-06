package com.xwab.app.core.catalogmanifest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The manifest is hand-written data, so its invariants are asserted rather than assumed: a wrong
 * category id or a duplicated source would otherwise surface as an empty screen on a device.
 */
class CatalogManifestTest {
    @Test
    fun everyCategoryHasThreeOrMoreTracksAndAnAccurateCount() {
        catalogCategories.forEach { category ->
            val tracks = catalogEntries.filter { it.music.categoryId == category.id }
            assertTrue(tracks.size >= 3, "${category.id} should have at least three tracks")
            assertEquals(tracks.size, category.musicCount)
        }
    }

    @Test
    fun catalogIdsSourcesAndCategoryReferencesAreValidAndUnique() {
        val musicIds = catalogEntries.map { it.music.id }
        val categoryIds = catalogCategories.map { it.id }.toSet()
        val httpsUrls = catalogEntries.map { it.httpsUrl }

        assertEquals(musicIds.size, musicIds.toSet().size, "duplicate music ids")
        assertTrue(catalogEntries.all { it.music.categoryId in categoryIds })
        assertEquals(httpsUrls.size, httpsUrls.toSet().size, "duplicate source URLs")
        // Only the suffix: `CatalogEntry.init` already refuses anything that is not HTTPS.
        assertTrue(httpsUrls.all { it.endsWith(".mp3") })
    }

    /**
     * Uniqueness is the half no entry can check on its own — [CatalogEntry] rejects a name the file
     * store would refuse, but only the whole manifest can say whether two tracks collide on one.
     */
    @Test
    fun everyTrackCachesUnderItsOwnFileName() {
        val cacheFileNames = catalogEntries.map { it.cacheFileName }

        assertEquals(cacheFileNames.size, cacheFileNames.toSet().size, "duplicate cache file names")
    }
}
