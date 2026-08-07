package com.xwab.app.feature.sounds.domain

import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackSummary
import com.xwab.app.core.testing.FakeFavorites
import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.core.testing.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlayerUseCasesTest {
    private val rain = track("gentle-rain", categoryId = "rain")
    private val catalog = FakeMusicCatalog(tracks = listOf(rain))

    @Test
    fun thePlayerScreenCombinesTrackFavoritesPlaybackAndSleepTimer() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
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
        val useCase = ObservePlayerContentUseCase(catalog, FakeFavorites(), FakePlaybackCoordinator())

        val content = useCase(TrackId("no-such-track")).first()

        assertNull(content.music)
        assertNull(content.sleepTimerRemainingMs)
    }
}
