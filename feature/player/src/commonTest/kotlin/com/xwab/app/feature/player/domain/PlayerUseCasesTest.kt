package com.xwab.app.feature.player.domain

import com.xwab.app.core.domain.port.PlaybackSummary
import com.xwab.app.core.testing.FakeFavorites
import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.core.testing.track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerUseCasesTest {
    private val rain = track("gentle-rain", categoryId = "rain")
    private val catalog = FakeMusicCatalog(tracks = listOf(rain))

    @Test
    fun thePlayerScreenCombinesTrackFavoritesPlaybackAndSleepTimer() = runBlocking {
        val coordinator = FakePlaybackCoordinator()
        coordinator.publish(PlaybackSummary(activeSourceId = "gentle-rain", isPlaying = true))
        coordinator.publishSleepTimer(90_000L)
        val useCase = ObservePlayerContentUseCase(
            catalog,
            FakeFavorites(setOf("gentle-rain")),
            coordinator,
        )

        val content = useCase("gentle-rain").first()

        assertEquals(rain, content.music)
        assertEquals(setOf("gentle-rain"), content.favoriteIds)
        assertTrue(content.playback.isPlaying)
        assertEquals(90_000L, content.sleepTimerRemainingMs)
    }

    @Test
    fun thePlayerScreenReportsAnUnknownTrackAsMissingInsteadOfFailing() = runBlocking {
        val useCase = ObservePlayerContentUseCase(catalog, FakeFavorites(), FakePlaybackCoordinator())

        val content = useCase("no-such-track").first()

        assertNull(content.music)
        assertNull(content.sleepTimerRemainingMs)
    }

    @Test
    fun thePlaybackSettingCommandsForwardToTheCoordinator() {
        val coordinator = FakePlaybackCoordinator()

        SetPlaybackLoopingUseCase(coordinator)(false)
        SetPlaybackVolumeUseCase(coordinator)(0.35f)

        assertEquals(false, coordinator.looping)
        assertEquals(0.35f, coordinator.volume)
    }

    @Test
    fun theSleepTimerCommandsForwardToTheCoordinator() {
        val coordinator = FakePlaybackCoordinator()

        StartSleepTimerUseCase(coordinator)(15 * 60_000L)
        CancelSleepTimerUseCase(coordinator)()

        assertEquals(15 * 60_000L, coordinator.startedTimerMs)
        assertEquals(1, coordinator.cancelledTimers)
    }
}
