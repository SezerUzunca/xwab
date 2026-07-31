package com.xwab.app.core.audiocontent

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
        assertTrue(httpsUrls.all { it.startsWith("https://") && it.endsWith(".mp3") })
    }

    /**
     * The cache file name is what the file store validates and what deduplicates downloads, so a
     * collision would make two tracks share one cached file.
     */
    @Test
    fun everyTrackCachesUnderItsOwnValidFileName() {
        val cacheFileNames = catalogEntries.map { it.cacheFileName }

        assertEquals(cacheFileNames.size, cacheFileNames.toSet().size, "duplicate cache file names")
        assertTrue(
            cacheFileNames.all { CACHE_FILE_NAME.matches(it) },
            "a cache file name would be rejected by the file store: $cacheFileNames",
        )
    }
}
