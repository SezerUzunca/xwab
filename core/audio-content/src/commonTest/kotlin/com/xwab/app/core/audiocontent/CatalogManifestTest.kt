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

        assertEquals(musicIds.size, musicIds.toSet().size, "duplicate music ids")
        assertTrue(catalogEntries.all { it.music.categoryId in categoryIds })

        val bundledPaths = catalogEntries.mapNotNull {
            (it.asset as? AudioAssetSpec.Bundled)?.resourcePath
        }
        val remoteUrls = catalogEntries.mapNotNull {
            (it.asset as? AudioAssetSpec.Remote)?.httpsUrl
        }
        assertEquals(5, bundledPaths.size)
        assertEquals(10, remoteUrls.size)
        assertEquals(bundledPaths.size, bundledPaths.toSet().size)
        assertEquals(remoteUrls.size, remoteUrls.toSet().size)
        assertTrue(remoteUrls.all { it.startsWith("https://") && it.endsWith(".mp3") })
    }
}
