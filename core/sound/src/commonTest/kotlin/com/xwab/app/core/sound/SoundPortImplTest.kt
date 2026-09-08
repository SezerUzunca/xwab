package com.xwab.app.core.sound

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SoundPortImplTest {
    private val rain = track("gentle-rain", "rain")
    private val waves = track("calm-waves", "ocean")
    private val adapter = SoundPortImpl(
        entries = listOf(rain, waves).map(::entry),
        categories = listOf(category("rain", musicCount = 1), category("ocean", musicCount = 1)),
    )

    @Test
    fun theWholeCatalogIsServedAsGiven() = runBlocking {
        assertEquals(listOf(rain, waves), adapter.observeAllMusic().first())
        assertEquals(
            listOf(CategoryId("rain"), CategoryId("ocean")),
            adapter.observeCategories().first().map { it.id },
        )
    }

    @Test
    fun aCategoryIsServedWithOnlyItsOwnTracks() = runBlocking {
        assertEquals(CategoryId("rain"), adapter.observeCategory(CategoryId("rain")).first()?.id)
        assertEquals(listOf(rain), adapter.observeMusicForCategory(CategoryId("rain")).first())
    }

    @Test
    fun oneTrackIsServedById() = runBlocking {
        assertEquals(waves, adapter.observeMusic(TrackId("calm-waves")).first())
    }

    @Test
    fun anUnknownIdEmitsNothingRatherThanNeverEmitting() = runBlocking {
        assertNull(adapter.observeMusic(TrackId("no-such-track")).first())
        assertNull(adapter.observeCategory(CategoryId("no-such-category")).first())
        assertEquals(
            emptyList<Music>(),
            adapter.observeMusicForCategory(CategoryId("no-such-category")).first(),
        )
    }

    @Test
    fun twoTracksUnderOneIdAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            SoundPortImpl(
                entries = listOf(rain, track("gentle-rain", "ocean")).map(::entry),
                categories = emptyList(),
            )
        }
    }

    private fun entry(music: Music) = CatalogEntry(music, "https://example.test/${music.id.value}.mp3")

    private fun track(id: String, categoryId: String) = Music(
        id = TrackId(id),
        name = id,
        categoryId = CategoryId(categoryId),
        durationSeconds = 60,
    )

    private fun category(id: String, musicCount: Int) = Category(
        id = CategoryId(id),
        name = id,
        description = "",
        symbol = "*",
        musicCount = musicCount,
    )
}
