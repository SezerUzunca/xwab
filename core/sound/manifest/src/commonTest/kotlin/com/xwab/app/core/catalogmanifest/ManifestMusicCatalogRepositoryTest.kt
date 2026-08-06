package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ManifestMusicCatalogRepositoryTest {
    private val rain = track("gentle-rain", "rain")
    private val waves = track("calm-waves", "ocean")
    private val repository = ManifestMusicCatalogRepository(
        tracks = listOf(rain, waves),
        categories = listOf(category("rain", musicCount = 1), category("ocean", musicCount = 1)),
    )

    @Test
    fun theWholeCatalogIsServedAsGiven() = runBlocking {
        assertEquals(listOf(rain, waves), repository.observeAllMusic().first())
        assertEquals(listOf("rain", "ocean"), repository.observeCategories().first().map { it.id })
    }

    @Test
    fun aCategoryIsServedWithOnlyItsOwnTracks() = runBlocking {
        assertEquals("rain", repository.observeCategory("rain").first()?.id)
        assertEquals(listOf(rain), repository.observeMusicForCategory("rain").first())
    }

    @Test
    fun oneTrackIsServedById() = runBlocking {
        assertEquals(waves, repository.observeMusic(TrackId("calm-waves")).first())
    }

    /**
     * A screen opened on a deleted track — a restored back stack, a stale deep link — has to see an
     * empty result rather than a flow that never emits.
     */
    @Test
    fun anUnknownIdEmitsNothingRatherThanNeverEmitting() = runBlocking {
        assertNull(repository.observeMusic(TrackId("no-such-track")).first())
        assertNull(repository.observeCategory("no-such-category").first())
        assertEquals(emptyList<Music>(), repository.observeMusicForCategory("no-such-category").first())
    }

    private fun track(id: String, categoryId: String) = Music(
        id = TrackId(id),
        name = id,
        categoryId = categoryId,
        durationSeconds = 60,
    )

    private fun category(id: String, musicCount: Int) = Category(
        id = id,
        name = id,
        description = "",
        symbol = "*",
        musicCount = musicCount,
    )
}
