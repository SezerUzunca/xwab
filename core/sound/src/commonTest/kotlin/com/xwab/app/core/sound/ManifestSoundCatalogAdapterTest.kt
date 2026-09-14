package com.xwab.app.core.sound

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ManifestSoundCatalogAdapterTest {
    private val rain = track("gentle-rain", "rain")
    private val waves = track("calm-waves", "ocean")
    private val adapter = ManifestSoundCatalogAdapter(
        tracks = listOf(rain, waves),
        categories = listOf(category("rain", trackCount = 1), category("ocean", trackCount = 1)),
    )

    @Test
    fun theWholeCatalogIsServedAsGiven() = runBlocking {
        assertEquals(listOf(rain, waves), adapter.observeAllTracks().first())
        assertEquals(
            listOf(CategoryId("rain"), CategoryId("ocean")),
            adapter.observeCategories().first().map { it.id },
        )
    }

    @Test
    fun aCategoryIsServedWithOnlyItsOwnTracks() = runBlocking {
        assertEquals(CategoryId("rain"), adapter.observeCategory(CategoryId("rain")).first()?.id)
        assertEquals(listOf(rain), adapter.observeTracksForCategory(CategoryId("rain")).first())
    }

    @Test
    fun oneTrackIsServedById() = runBlocking {
        assertEquals(waves, adapter.observeTrack(TrackId("calm-waves")).first())
    }

    @Test
    fun anUnknownIdEmitsNothingRatherThanNeverEmitting() = runBlocking {
        assertNull(adapter.observeTrack(TrackId("no-such-track")).first())
        assertNull(adapter.observeCategory(CategoryId("no-such-category")).first())
        assertEquals(
            emptyList<Track>(),
            adapter.observeTracksForCategory(CategoryId("no-such-category")).first(),
        )
    }

    @Test
    fun twoTracksUnderOneIdAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ManifestSoundCatalogAdapter(
                tracks = listOf(rain, track("gentle-rain", "ocean")),
                categories = emptyList(),
            )
        }
    }

    private fun track(id: String, categoryId: String) = Track(
        id = TrackId(id),
        name = id,
        categoryId = CategoryId(categoryId),
        durationSeconds = 60,
    )

    private fun category(id: String, trackCount: Int) = Category(
        id = CategoryId(id),
        name = id,
        description = "",
        symbol = "*",
        trackCount = trackCount,
    )
}
