package com.xwab.app.feature.sound.domain

import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ObserveSoundContentUseCaseTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun offlineReadinessUpdatesWithoutStartingPlayback() = runTest {
        val soundCatalog = FakeSoundCatalog(tracks = listOf(rain))
        val playback = FakePlaybackPort()
        val emissions = mutableListOf<SoundContent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveSoundContentUseCase(soundCatalog, FakeFavorites(), playback)(rain.id).toList(emissions)
        }
        runCurrent()
        assertFalse(emissions.last().availableOffline)
        soundCatalog.setOfflineReady(rain.id, true)
        runCurrent()
        assertTrue(emissions.last().availableOffline)
        soundCatalog.setOfflineReady(rain.id, false)
        runCurrent()
        assertFalse(emissions.last().availableOffline)
        assertNull(playback.playedItemId)
        assertEquals(0, playback.pauses)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun unrelatedFavoritesAndTimerDoNotRepublishButAvailabilityDoes() = runTest {
        val favorites = FakeFavorites(setOf(rain.id))
        val playback = FakePlaybackPort()
        val emissions = mutableListOf<SoundContent>()
        val useCase = ObserveSoundContentUseCase(catalog, favorites, playback)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(rain.id).filter { it.favoriteReadStatus != SoundFavoriteReadStatus.Pending }.toList(emissions)
        }
        runCurrent()
        assertTrue(emissions.single().isFavorite)

        favorites.toggle(SOUND_FAVORITES_NAMESPACE, "other-sound")
        runCurrent()
        assertEquals(1, emissions.size)

        playback.publishSleepTimer(59_000L)
        runCurrent()
        assertEquals(1, emissions.size)
        assertTrue(emissions.last().isFavorite)

        favorites.available.value = false
        runCurrent()
        assertEquals(SoundFavoriteReadStatus.Unavailable, emissions.last().favoriteReadStatus)

        favorites.available.value = true
        favorites.toggle(SOUND_FAVORITES_NAMESPACE, rain.id.value)
        runCurrent()
        assertEquals(SoundFavoriteReadStatus.Available, emissions.last().favoriteReadStatus)
        assertFalse(emissions.last().isFavorite)
    }

    private val rain = track("gentle-rain", categoryId = "rain")
    private val catalog = FakeSoundCatalog(tracks = listOf(rain))

    @Test
    fun theSoundScreenCombinesTrackFavoritesAndPlayback() = runBlocking {
        val coordinator = FakePlaybackPort()
        coordinator.publish(
            PlaybackSummary(requestedItemId = PlaybackItemId(SOUND_PLAYBACK_KIND, "gentle-rain"), playIntent = true, isPlaying = true),
        )
        coordinator.publishSleepTimer(90_000L)
        val useCase = ObserveSoundContentUseCase(
            catalog,
            FakeFavorites(setOf(TrackId("gentle-rain"))),
            coordinator,
        )

        val content = useCase(TrackId("gentle-rain")).first { it.favoriteReadStatus == SoundFavoriteReadStatus.Available }

        assertEquals(rain, content.track)
        assertTrue(content.isFavorite)
        assertTrue(content.playback.isPlaying)
    }

    @Test
    fun theSoundScreenReportsAnUnknownTrackAsMissingInsteadOfFailing() = runBlocking {
        val useCase = ObserveSoundContentUseCase(catalog, FakeFavorites(), FakePlaybackPort())

        val content = useCase(TrackId("no-such-track")).first()

        assertNull(content.track)
    }
}
