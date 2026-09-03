package com.xwab.app.feature.favorites.impl

import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackFailure
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackSummary
import com.xwab.app.core.testing.FakeFavorites
import com.xwab.app.core.testing.FakeMusicCatalog
import com.xwab.app.core.testing.FakePlaybackCoordinator
import com.xwab.app.core.testing.track
import com.xwab.app.core.ui.state.Loadable
import com.xwab.app.feature.favorites.impl.domain.ObserveFavoritesContentUseCase
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
        val coordinator = FakePlaybackCoordinator().apply {
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
        val coordinator = FakePlaybackCoordinator().apply {
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
        val coordinator = FakePlaybackCoordinator().apply {
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
        val coordinator = FakePlaybackCoordinator()
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(TrackId("rain"))
        advanceUntilIdle()

        assertEquals(PlaybackItemId.sound("rain"), coordinator.playedItemId)
        assertTrue(coordinator.pauses == 0)
    }

    private fun createViewModel(coordinator: FakePlaybackCoordinator): FavoritesViewModel {
        val useCase = ObserveFavoritesContentUseCase(
            musicCatalog = FakeMusicCatalog(tracks = listOf(track("rain"))),
            favoritesRepository = FakeFavorites(setOf(TrackId("rain"))),
            playbackCoordinator = coordinator,
        )
        return FavoritesViewModel(useCase, coordinator)
    }

    private fun TestScope.collectState(viewModel: FavoritesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }
}
