package com.xwab.app.feature.category

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoritesSnapshot
import com.xwab.app.feature.category.domain.CategoryFavoritesReadStatus
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.category
import com.xwab.app.testing.track
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

/**
 * What this screen decides for itself: which row the session is on, and what a tap on a row does.
 * The join behind it is covered by the use case's own test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * A sound and a story are allowed to share a raw id, which is why the session's item carries a
     * kind. The session being on the *story* called `gentle-rain` must leave the *sound* called
     * `gentle-rain` as idle as every other row here: no row named, no row playing, and a tap that
     * starts playback rather than pausing something this screen never showed.
     */
    @Test
    fun aStoryInTheSessionLeavesEveryRowIdle() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.story("gentle-rain"),
                    playIntent = true,
                    isPreparing = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertNull(state.requestedTrackId)
        assertFalse(state.playIntent)

        viewModel.togglePlayback(GENTLE_RAIN)
        advanceUntilIdle()

        assertEquals(PlaybackItemId.sound("gentle-rain"), port.playedItemId)
        assertEquals(0, port.pauses)
    }

    @Test
    fun tappingTheRequestedPlayingSoundPauses() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.sound("gentle-rain"),
                    playIntent = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(GENTLE_RAIN)

        assertEquals(1, port.pauses)
        assertNull(port.playedItemId)
    }

    /** A tap on a different row moves the session rather than pausing the row already playing. */
    @Test
    fun tappingAnotherRowRequestsItInstead() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.sound("gentle-rain"),
                    playIntent = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(HEAVY_RAIN)
        advanceUntilIdle()

        assertEquals(PlaybackItemId.sound("heavy-rain"), port.playedItemId)
        assertEquals(0, port.pauses)
    }

    @Test
    fun togglingAFavoriteReachesTheFavoritesPort() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(GENTLE_RAIN))
        val viewModel = createViewModel(FakePlaybackPort(), favorites)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.toggleFavorite(HEAVY_RAIN)
        advanceUntilIdle()

        assertEquals(listOf(SOUND_FAVORITES_NAMESPACE to HEAVY_RAIN.value), favorites.toggles)
    }

    /**
     * Which row the session is on is the state's answer, not a comparison spelled out again by
     * whatever draws the list.
     */
    @Test
    fun onlyTheRequestedRowReadsAsPlayingOrPreparing() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.sound("gentle-rain"),
                    playIntent = true,
                    isPreparing = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertTrue(state.isRowPlaying(GENTLE_RAIN))
        assertTrue(state.isRowPreparing(GENTLE_RAIN))
        assertFalse(state.isRowPlaying(HEAVY_RAIN))
        assertFalse(state.isRowPreparing(HEAVY_RAIN))
    }

    /**
     * A row here starts playback like a favorites row does, so it now reports what came of that the
     * same way — against the failure's own track, since a failed lookup has already released the
     * session's claim on it.
     */
    @Test
    fun aFailureReachesOnlyTheRowItHappenedTo() = runTest(mainDispatcher) {
        val failure = PlaybackFailure.SourceUnavailable(PlaybackItemId.sound("gentle-rain"))
        val port = FakePlaybackPort().apply { publish(PlaybackSummary(failure = failure)) }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertEquals(failure, state.rowFailure(GENTLE_RAIN))
        assertNull(state.rowFailure(HEAVY_RAIN))
    }

    /** A story that failed is not this screen's to report, even sharing a row's raw id. */
    @Test
    fun aStorysFailureReachesNoRow() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    failure = PlaybackFailure.SourceUnavailable(PlaybackItemId.story("gentle-rain")),
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertNull(readyState(viewModel).rowFailure(GENTLE_RAIN))
    }

    @Test
    fun favoriteReadFailuresAreVisibleAndRecoverWithoutReopeningTheScreen() = runTest(mainDispatcher) {
        val favorites = FakeFavorites()
        val viewModel = createViewModel(FakePlaybackPort(), favorites = favorites)
        collectState(viewModel)
        advanceUntilIdle()
        favorites.available.value = false
        advanceUntilIdle()
        assertFalse(readyState(viewModel).favoritesAvailable)
        favorites.available.value = true
        advanceUntilIdle()
        assertTrue(readyState(viewModel).favoritesAvailable)
    }

    @Test
    fun favoriteWriteFailuresAreVisibleUntilASuccessfulRetry() = runTest(mainDispatcher) {
        val favorites = FakeFavorites().apply { toggleResult = FavoriteToggleResult.Unavailable }
        val viewModel = createViewModel(FakePlaybackPort(), favorites = favorites)
        collectState(viewModel)
        advanceUntilIdle()
        viewModel.toggleFavorite(HEAVY_RAIN)
        advanceUntilIdle()
        assertTrue(readyState(viewModel).favoriteWriteFailed)
        favorites.toggleResult = FavoriteToggleResult.Updated
        viewModel.toggleFavorite(HEAVY_RAIN)
        advanceUntilIdle()
        assertFalse(readyState(viewModel).favoriteWriteFailed)
    }
    @Test
    fun categoryAndPlaybackAreUsableBeforeTheFirstFavoriteRead() = runTest(mainDispatcher) {
        val reads = MutableSharedFlow<FavoritesSnapshot>(replay = 1)
        val writes = FakeFavorites()
        val playback = FakePlaybackPort()
        val viewModel = createViewModel(playback, favoritesReading(reads, writes))
        collectState(viewModel)
        runCurrent()

        val pending = readyState(viewModel)
        assertEquals(RAIN, pending.category?.id)
        assertEquals(listOf(GENTLE_RAIN, HEAVY_RAIN), pending.tracks.map { it.id })
        assertEquals(CategoryFavoritesReadStatus.Pending, pending.favoritesReadStatus)
        assertFalse(pending.favoritesAvailable)
        viewModel.toggleFavorite(GENTLE_RAIN)
        viewModel.togglePlayback(GENTLE_RAIN)
        runCurrent()
        assertTrue(writes.toggles.isEmpty())
        assertEquals(PlaybackItemId.sound(GENTLE_RAIN.value), playback.playedItemId)

        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        runCurrent()
        assertEquals(CategoryFavoritesReadStatus.Unavailable, readyState(viewModel).favoritesReadStatus)

        reads.emit(FavoritesSnapshot(setOf(GENTLE_RAIN.value)))
        runCurrent()
        assertTrue(readyState(viewModel).isRowFavorite(GENTLE_RAIN))
        assertTrue(readyState(viewModel).favoritesAvailable)
    }

    @Test
    fun favoriteMembershipSurvivesAStoppedSubscriptionAndFailedFirstRead() = runTest(mainDispatcher) {
        val reads = MutableSharedFlow<FavoritesSnapshot>(replay = 1)
        reads.emit(FavoritesSnapshot(setOf(GENTLE_RAIN.value)))
        val viewModel = createViewModel(FakePlaybackPort(), favoritesReading(reads))
        val firstCollection = collectState(viewModel)
        runCurrent()
        assertEquals(setOf(GENTLE_RAIN), readyState(viewModel).favoriteIds)

        firstCollection.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, reads.subscriptionCount.value)
        reads.resetReplayCache()
        val secondCollection = collectState(viewModel)
        runCurrent()
        assertEquals(CategoryFavoritesReadStatus.Pending, readyState(viewModel).favoritesReadStatus)
        assertEquals(setOf(GENTLE_RAIN), readyState(viewModel).favoriteIds)

        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        runCurrent()
        assertFalse(readyState(viewModel).favoritesAvailable)
        assertEquals(setOf(GENTLE_RAIN), readyState(viewModel).favoriteIds)

        reads.emit(FavoritesSnapshot(emptySet()))
        runCurrent()
        assertTrue(readyState(viewModel).favoriteIds.isEmpty())
        assertTrue(readyState(viewModel).favoritesAvailable)

        secondCollection.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        collectState(viewModel)
        runCurrent()
        assertTrue(readyState(viewModel).favoriteIds.isEmpty())
        assertFalse(readyState(viewModel).favoritesAvailable)
    }

    @Test
    fun favoritesAndPlaybackOutsideThisCategoryDoNotChangeTheScreenState() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(GENTLE_RAIN, TrackId("waves")))
        val playback = FakePlaybackPort()
        val viewModel = createViewModel(playback, favorites)
        val emissions = mutableListOf<CategoryUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.toList(emissions) }
        runCurrent()
        val before = emissions.size
        assertEquals(setOf(GENTLE_RAIN), readyState(viewModel).favoriteIds)

        favorites.toggle(SOUND_FAVORITES_NAMESPACE, "birds")
        runCurrent()
        playback.publish(PlaybackSummary(
            requestedItemId = PlaybackItemId.sound("waves"),
            playIntent = true,
            isPreparing = true,
            failure = PlaybackFailure.SourceUnavailable(PlaybackItemId.sound("birds")),
        ))
        runCurrent()
        assertEquals(before, emissions.size)

        favorites.toggle(SOUND_FAVORITES_NAMESPACE, HEAVY_RAIN.value)
        runCurrent()
        assertTrue(readyState(viewModel).isRowFavorite(HEAVY_RAIN))
    }

    private fun favoritesReading(reads: Flow<FavoritesSnapshot>, writes: FakeFavorites = FakeFavorites()): FavoritesPort =
        object : FavoritesPort by writes {
            override fun observe(namespace: String): Flow<FavoritesSnapshot> = reads
        }

    private fun createViewModel(
        port: FakePlaybackPort,
        favorites: FavoritesPort = FakeFavorites(setOf(GENTLE_RAIN)),
    ): CategoryViewModel {
        val catalog = FakeSoundCatalog(
            categories = listOf(category("rain", trackCount = 2)),
            tracks = listOf(track("gentle-rain"), track("heavy-rain")),
        )
        val useCase = ObserveCategoryContentUseCase(
            soundPort = catalog,
            favoritesPort = favorites,
            playbackPort = port,
        )
        return CategoryViewModel(RAIN, useCase, favorites, port)
    }

    private fun readyState(viewModel: CategoryViewModel): CategoryState =
        assertIs<CategoryUiState.Ready>(viewModel.state.value).value

    private fun TestScope.collectState(viewModel: CategoryViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

    private companion object {
        val RAIN = CategoryId("rain")
        val GENTLE_RAIN = TrackId("gentle-rain")
        val HEAVY_RAIN = TrackId("heavy-rain")
    }
}
