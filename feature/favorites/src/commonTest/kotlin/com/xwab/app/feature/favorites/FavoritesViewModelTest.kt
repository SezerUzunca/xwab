package com.xwab.app.feature.favorites

import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoritesSnapshot
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
                    requestedItemId = PlaybackItemId(OTHER_KIND, "night"),
                    playIntent = true,
                    isPreparing = true,
                ),
            )
        }
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        val state = assertIs<FavoritesUiState.Ready>(viewModel.state.value).value
        assertEquals(listOf(TrackId("rain")), state.tracks.map { it.id })
        assertNull(state.requestedTrackId)
        assertFalse(state.playIntent)
        assertFalse(state.isPreparing)
    }

    @Test
    fun attachesPlaybackFailureToItsSound() = runTest(mainDispatcher) {
        val itemId = PlaybackItemId(SOUND_PLAYBACK_KIND, "rain")
        val coordinator = FakePlaybackPort().apply {
            publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(itemId)))
        }
        val viewModel = createViewModel(coordinator)
        collectState(viewModel)
        advanceUntilIdle()

        val state = assertIs<FavoritesUiState.Ready>(viewModel.state.value).value
        assertEquals(PlaybackFailure.SourceUnavailable(itemId), state.playbackFailure)
        // Which row wears it is the state's answer too, and only the row it happened to.
        assertEquals(PlaybackFailure.SourceUnavailable(itemId), state.rowFailure(TrackId("rain")))
        assertNull(state.rowFailure(TrackId("ocean")))
    }

    @Test
    fun tappingRequestedPlayingSoundPauses() = runTest(mainDispatcher) {
        val coordinator = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"),
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

        assertEquals(PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"), coordinator.playedItemId)
        assertTrue(coordinator.pauses == 0)
    }

    @Test
    fun unavailableFavoritesKeepTheirLastKnownRowsAndRecover() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(TrackId("rain")))
        val viewModel = createViewModel(FakePlaybackPort(), favorites)
        collectState(viewModel)
        advanceUntilIdle()
        favorites.available.value = false
        advanceUntilIdle()
        val unavailable = assertIs<FavoritesUiState.Ready>(viewModel.state.value).value
        assertFalse(unavailable.favoritesAvailable)
        assertEquals(listOf(TrackId("rain")), unavailable.tracks.map { it.id })
        favorites.available.value = true
        advanceUntilIdle()
        assertTrue(assertIs<FavoritesUiState.Ready>(viewModel.state.value).value.favoritesAvailable)
    }
    @Test
    fun rowsSurviveAStoppedSubscriptionAndFailedFirstReadUntilASuccessfulEmptyRead() = runTest(mainDispatcher) {
        val reads = MutableSharedFlow<FavoritesSnapshot>(replay = 1)
        reads.emit(FavoritesSnapshot(setOf("rain")))
        val playback = FakePlaybackPort()
        val viewModel = createViewModel(playback, favoritesReading(reads))
        val firstCollection = collectState(viewModel)
        runCurrent()
        assertEquals(listOf(TrackId("rain")), readyState(viewModel).tracks.map { it.id })

        firstCollection.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, reads.subscriptionCount.value)
        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        val secondCollection = collectState(viewModel)
        runCurrent()
        assertEquals(listOf(TrackId("rain")), readyState(viewModel).tracks.map { it.id })
        assertFalse(readyState(viewModel).favoritesAvailable)

        playback.publish(PlaybackSummary(
            requestedItemId = PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"),
            playIntent = true,
            isPreparing = true,
        ))
        runCurrent()
        assertTrue(readyState(viewModel).isRowPreparing(TrackId("rain")))
        viewModel.togglePlayback(TrackId("rain"))
        assertEquals(1, playback.pauses)

        reads.emit(FavoritesSnapshot(emptySet()))
        runCurrent()
        assertTrue(readyState(viewModel).tracks.isEmpty())
        assertTrue(readyState(viewModel).favoritesAvailable)
        assertNull(readyState(viewModel).requestedTrackId)

        secondCollection.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        collectState(viewModel)
        runCurrent()
        assertTrue(readyState(viewModel).tracks.isEmpty())
        assertFalse(readyState(viewModel).favoritesAvailable)
    }

    @Test
    fun playbackOutsideTheFavoriteRowsDoesNotChangeTheScreenState() = runTest(mainDispatcher) {
        val playback = FakePlaybackPort()
        val viewModel = createViewModel(playback)
        val emissions = mutableListOf<FavoritesUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.toList(emissions) }
        runCurrent()
        val before = emissions.size

        playback.publish(PlaybackSummary(
            requestedItemId = PlaybackItemId(SOUND_PLAYBACK_KIND, "ocean"),
            playIntent = true,
            isPreparing = true,
            failure = PlaybackFailure.SourceUnavailable(PlaybackItemId(SOUND_PLAYBACK_KIND, "birds")),
        ))
        runCurrent()
        assertEquals(before, emissions.size)

        val failure = PlaybackFailure.SourceUnavailable(PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"))
        playback.publish(PlaybackSummary(failure = failure))
        runCurrent()
        assertEquals(failure, readyState(viewModel).rowFailure(TrackId("rain")))
    }

    @Test
    fun firstReadFailureIsUnavailableAndRecoversToARealEmptyList() = runTest(mainDispatcher) {
        val reads = MutableSharedFlow<FavoritesSnapshot>(replay = 1)
        val viewModel = createViewModel(FakePlaybackPort(), favoritesReading(reads))
        collectState(viewModel)
        runCurrent()
        assertIs<FavoritesUiState.Loading>(viewModel.state.value)

        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        runCurrent()
        assertTrue(readyState(viewModel).tracks.isEmpty())
        assertFalse(readyState(viewModel).favoritesAvailable)

        reads.emit(FavoritesSnapshot(emptySet()))
        runCurrent()
        assertTrue(readyState(viewModel).tracks.isEmpty())
        assertTrue(readyState(viewModel).favoritesAvailable)
    }

    private fun favoritesReading(reads: Flow<FavoritesSnapshot>): FavoritesPort =
        object : FavoritesPort by FakeFavorites() {
            override fun observe(namespace: String): Flow<FavoritesSnapshot> = reads
        }

    private fun readyState(viewModel: FavoritesViewModel): FavoritesState =
        assertIs<FavoritesUiState.Ready>(viewModel.state.value).value

    private fun createViewModel(coordinator: FakePlaybackPort, favorites: FavoritesPort = FakeFavorites(setOf(TrackId("rain")))): FavoritesViewModel {
        val useCase = ObserveFavoritesContentUseCase(
            soundPort = FakeSoundCatalog(tracks = listOf(track("rain"))),
            favoritesPort = favorites,
            playbackPort = coordinator,
        )
        return FavoritesViewModel(useCase, coordinator)
    }

    private fun TestScope.collectState(viewModel: FavoritesViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
}

/** Some kind this screen does not show, to prove it ignores one. */
private const val OTHER_KIND = "other-kind"
