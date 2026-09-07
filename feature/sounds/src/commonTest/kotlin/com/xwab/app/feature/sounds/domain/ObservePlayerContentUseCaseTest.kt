package com.xwab.app.feature.sounds.domain

import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.playback.port.PlaybackItemId
import com.xwab.app.core.playback.port.PlaybackSummary
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeMusicCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ObservePlayerContentUseCaseTest {
    private val rain = track("gentle-rain", categoryId = "rain")
    private val catalog = FakeMusicCatalog(tracks = listOf(rain))

    @Test
    fun thePlayerScreenCombinesTrackFavoritesPlaybackAndSleepTimer() = runBlocking {
        val coordinator = FakePlaybackPort()
        coordinator.publish(
            PlaybackSummary(requestedItemId = PlaybackItemId.sound("gentle-rain"), playIntent = true, isPlaying = true),
        )
        coordinator.publishSleepTimer(90_000L)
        val useCase = ObservePlayerContentUseCase(
            catalog,
            FakeFavorites(setOf(TrackId("gentle-rain"))),
            coordinator,
        )

        val content = useCase(TrackId("gentle-rain")).first()

        assertEquals(rain, content.music)
        assertEquals(setOf(TrackId("gentle-rain")), content.favoriteIds)
        assertTrue(content.playback.isPlaying)
        assertEquals(90_000L, content.sleepTimerRemainingMs)
    }

    @Test
    fun thePlayerScreenReportsAnUnknownTrackAsMissingInsteadOfFailing() = runBlocking {
        val useCase = ObservePlayerContentUseCase(catalog, FakeFavorites(), FakePlaybackPort())

        val content = useCase(TrackId("no-such-track")).first()

        assertNull(content.music)
        assertNull(content.sleepTimerRemainingMs)
    }
}
