package com.xwab.app.core.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The manifest is hand-written data, so its invariants are asserted rather than assumed: a wrong
 * category id or duplicate metadata would otherwise surface as an empty screen on a device.
 */
class CatalogManifestTest {
    @Test
    fun everyCategoryHasFourOrMoreTracksAndAnAccurateCount() {
        catalogCategories.forEach { category ->
            val tracks = catalogManifest.filter { it.categoryId == category.id }
            assertTrue(tracks.size >= 4, "${category.id} should have at least four tracks")
            assertEquals(tracks.size, category.trackCount)
        }
    }

    @Test
    fun catalogIdsAndCategoryReferencesAreValidAndUnique() {
        val trackIds = catalogManifest.map { it.id }
        val categoryIds = catalogCategories.map { it.id }.toSet()

        assertEquals(trackIds.size, trackIds.toSet().size, "duplicate track ids")
        assertTrue(catalogManifest.all { it.categoryId in categoryIds })
        assertTrue(catalogManifest.all { it.durationSeconds > 0 })
    }
}
