package com.xwab.app.core.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BundledMusicCatalogRepositoryTest {
    private val repository = BundledMusicCatalogRepository()

    @Test
    fun everyCategoryHasMatchingMusic() = runBlocking {
        repository.observeCategories().first().forEach { category ->
            val music = repository.observeMusicForCategory(category.id).first()
            assertTrue(music.isNotEmpty(), "${category.id} must contain audio")
            assertEquals(music.size, category.musicCount)
            assertTrue(music.all { it.categoryId == category.id })
        }
    }

    @Test
    fun everyTrackBelongsToAKnownCategory() = runBlocking {
        val categoryIds = repository.observeCategories().first().map { it.id }.toSet()
        repository.observeAllMusic().first().forEach { music ->
            assertTrue(
                music.categoryId in categoryIds,
                "${music.id} points at the unknown category ${music.categoryId}",
            )
        }
    }

    @Test
    fun trackIdsAreUniqueAndResolvable() = runBlocking {
        val tracks = repository.observeAllMusic().first()

        assertEquals(tracks.size, tracks.map { it.id }.toSet().size, "duplicate track ids")
        tracks.forEach { music -> assertNotNull(repository.observeMusic(music.id).first()) }
    }

    @Test
    fun categoryIdsAreUniqueAndResolvable() = runBlocking {
        val categories = repository.observeCategories().first()

        assertEquals(categories.size, categories.map { it.id }.toSet().size, "duplicate category ids")
        categories.forEach { category -> assertNotNull(repository.observeCategory(category.id).first()) }
    }

    @Test
    fun everyTrackPointsAtADistinctBundledMp3() = runBlocking {
        val tracks = repository.observeAllMusic().first()

        tracks.forEach { music ->
            assertTrue(
                music.audioResource.startsWith("files/audio/") && music.audioResource.endsWith(".mp3"),
                "${music.id} does not point into the bundled audio folder: ${music.audioResource}",
            )
        }
        assertEquals(
            tracks.size,
            tracks.map { it.audioResource }.toSet().size,
            "two tracks share one audio file",
        )
    }

    @Test
    fun unknownIdsResolveToNothingRatherThanFailing() = runBlocking {
        assertNull(repository.observeMusic("no-such-track").first())
        assertNull(repository.observeCategory("no-such-category").first())
        assertTrue(repository.observeMusicForCategory("no-such-category").first().isEmpty())
    }
}
