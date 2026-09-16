package com.xwab.app.feature.favorites.domain

import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class ObserveFavoritesContentUseCaseTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun playbackReusesSavedTracksWhileFavoriteAndAvailabilityChangesStillPropagate() = runTest {
        val favorites = FakeFavorites(setOf(rain.id))
        val playback = FakePlaybackPort()
        val emissions = mutableListOf<FavoritesContent>()
        val useCase = ObserveFavoritesContentUseCase(catalog, favorites, playback)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().toList(emissions)
        }
        runCurrent()
        val savedTracks = emissions.single().tracks
        val playing = PlaybackSummary(requestedItemId = PlaybackItemId.sound(rain.id.value), playIntent = true)

        playback.publish(playing)
        runCurrent()
        assertEquals(playing, emissions.last().playback)
        assertSame(savedTracks, emissions.last().tracks)

        favorites.available.value = false
        runCurrent()
        assertFalse(emissions.last().favoritesAvailable)

        favorites.available.value = true
        favorites.toggle(SOUND_FAVORITES_NAMESPACE, waves.id.value)
        runCurrent()
        assertTrue(emissions.last().favoritesAvailable)
        assertEquals(listOf(rain, waves), emissions.last().tracks)
        assertEquals(playing, emissions.last().playback)
    }

    private val rain = track("gentle-rain", categoryId = "rain")
    private val waves = track("calm-waves", categoryId = "ocean")
    private val catalog = FakeSoundCatalog(tracks = listOf(rain, waves))

    @Test
    fun filtersFavoritesAndPreservesCatalogOrder() = runBlocking {
        val useCase = ObserveFavoritesContentUseCase(
            catalog,
            FakeFavorites(setOf(TrackId("calm-waves"), TrackId("gentle-rain"))),
            FakePlaybackPort(),
        )

        assertEquals(listOf(rain, waves), useCase().first().tracks)
    }

    @Test
    fun reflectsFavoriteChangesAfterTheFirstRead() = runBlocking {
        val favorites = FakeFavorites()
        val useCase = ObserveFavoritesContentUseCase(catalog, favorites, FakePlaybackPort())
        val emissions = mutableListOf<FavoritesContent>()
        val collection = launch { useCase().take(2).toList(emissions) }
        while (emissions.isEmpty()) yield()

        assertTrue(emissions.single().tracks.isEmpty())
        favorites.toggle(SOUND_FAVORITES_NAMESPACE, "calm-waves")
        collection.join()

        assertEquals(listOf(waves), emissions.last().tracks)
    }

    @Test
    fun carriesPlaybackSummaryThrough() = runBlocking {
        val coordinator = FakePlaybackPort()
        val playing = PlaybackSummary(
            requestedItemId = PlaybackItemId.sound("gentle-rain"),
            playIntent = true,
            isPlaying = true,
        )
        coordinator.publish(playing)
        val useCase = ObserveFavoritesContentUseCase(catalog, FakeFavorites(), coordinator)

        assertEquals(playing, useCase().first().playback)
    }
}
