package com.xwab.app.feature.category

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    private fun createViewModel(
        port: FakePlaybackPort,
        favorites: FakeFavorites = FakeFavorites(setOf(GENTLE_RAIN)),
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
        assertIs<Loadable.Ready<CategoryState>>(viewModel.state.value).value

    private fun TestScope.collectState(viewModel: CategoryViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }

    private companion object {
        val RAIN = CategoryId("rain")
        val GENTLE_RAIN = TrackId("gentle-rain")
        val HEAVY_RAIN = TrackId("heavy-rain")
    }
}
