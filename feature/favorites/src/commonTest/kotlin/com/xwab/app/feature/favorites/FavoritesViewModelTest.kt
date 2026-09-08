package com.xwab.app.feature.favorites

import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeMusicCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.feature.favorites.domain.ObserveFavoritesContentUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun ignoresStoryPlaybackAndMapsFavoriteSounds() = runTest(mainDispatcher) {
        val coordinator = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.story("night"),
                    playIntent = true,
                    isPreparing = true,
                ),
            )
        }
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        val state = assertIs<Loadable.Ready<FavoritesState>>(viewModel.state.value).value
        assertEquals(listOf(TrackId("rain")), state.musics.map { it.id })
        assertNull(state.requestedTrackId)
        assertFalse(state.playIntent)
        assertFalse(state.isPreparing)
    }

    @Test
    fun attachesPlaybackFailureToItsSound() = runTest(mainDispatcher) {
        val itemId = PlaybackItemId.sound("rain")
        val coordinator = FakePlaybackPort().apply {
            publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(itemId)))
        }
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        val state = assertIs<Loadable.Ready<FavoritesState>>(viewModel.state.value).value
        assertEquals(PlaybackFailure.SourceUnavailable(itemId), state.playbackFailure)
    }

    @Test
    fun tappingRequestedPlayingSoundPauses() = runTest(mainDispatcher) {
        val coordinator = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.sound("rain"),
                    playIntent = true,
                ),
            )
        }
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(TrackId("rain"))

        assertEquals(1, coordinator.pauses)
        assertNull(coordinator.playedItemId)
    }

    @Test
    fun tappingIdleSoundRequestsPlayback() = runTest(mainDispatcher) {
        val coordinator = FakePlaybackPort()
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(TrackId("rain"))
        advanceUntilIdle()

        assertEquals(PlaybackItemId.sound("rain"), coordinator.playedItemId)
        assertTrue(coordinator.pauses == 0)
    }

    private fun createViewModel(coordinator: FakePlaybackPort): FavoritesViewModel {
        val useCase = ObserveFavoritesContentUseCase(
            soundCatalogPort = FakeMusicCatalog(tracks = listOf(track("rain"))),
            favoritesPort = FakeFavorites(setOf(TrackId("rain"))),
            playbackPort = coordinator,
        )
        return FavoritesViewModel(useCase, coordinator)
    }

    private fun TestScope.collectState(viewModel: FavoritesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }
}
