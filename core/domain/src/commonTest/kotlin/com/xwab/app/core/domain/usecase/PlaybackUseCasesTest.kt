package com.xwab.app.core.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackUseCasesTest {
    private val rain = track("gentle-rain")
    private val catalog = FakeMusicCatalog(tracks = listOf(rain))

    @Test
    fun togglingPlaybackResolvesTheTrackBeforeHandingItToTheCoordinator() = runBlocking {
        val coordinator = FakePlaybackCoordinator()

        ToggleMusicPlaybackUseCase(catalog, coordinator)("gentle-rain")

        assertEquals(rain, coordinator.toggledTrack)
    }

    @Test
    fun togglingPlaybackOnAnUnknownTrackDoesNothing() = runBlocking {
        val coordinator = FakePlaybackCoordinator()

        ToggleMusicPlaybackUseCase(catalog, coordinator)("no-such-track")

        assertNull(coordinator.toggledTrack)
    }

    @Test
    fun togglingAFavoriteReachesTheRepository() = runBlocking {
        val favorites = FakeFavorites()

        ToggleFavoriteUseCase(favorites)("gentle-rain")

        assertEquals(listOf("gentle-rain"), favorites.toggles)
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
