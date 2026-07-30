package com.xwab.app.core.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToggleMusicPlaybackUseCaseTest {
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
}
