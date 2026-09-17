package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.testing.FakePlaybackPort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class NowPlayingViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aSessionThatHasNeverBeenAskedForAnythingShowsNothing() = runTest {
        val viewModel = NowPlayingViewModel(FakePlaybackPort())
        collectState(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.idle)
    }

    @Test
    fun theBarNamesWhatTheSessionIsOn() = runTest {
        val port = playing(
            PlaybackSummary(requestedItemId = RAIN, title = "Gentle Rain", playIntent = true),
        )
        val viewModel = NowPlayingViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.idle)
        assertEquals("Gentle Rain", state.title)
        assertTrue(state.playIntent)
    }

    /**
     * The session withholds a title for exactly as long as it is switching, so the bar has a name
     * for the incoming item or no name at all — never the outgoing one's.
     */
    @Test
    fun aSwitchIsShownWithoutANameUntilTheSessionHasOne() = runTest {
        val port = playing(
            PlaybackSummary(
                requestedItemId = WAVES,
                title = null,
                playIntent = true,
                isPreparing = true,
            ),
        )
        val viewModel = NowPlayingViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.idle, "the bar stays up across a switch")
        assertNull(state.title)
        assertTrue(state.isPreparing)
    }

    @Test
    fun aTapPausesWhatTheControlShowsAsPlaying() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, playIntent = true))
        val viewModel = NowPlayingViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback()
        advanceUntilIdle()

        assertEquals(1, port.pauses)
        assertNull(port.playedItemId)
    }

    @Test
    fun aTapResumesWhatTheControlShowsAsStopped() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, playIntent = false))
        val viewModel = NowPlayingViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback()
        advanceUntilIdle()

        assertEquals(RAIN, port.playedItemId)
        assertEquals(0, port.pauses)
    }

    /**
     * The bar draws nothing while the session is idle, so this can only be reached by a tap racing
     * the state it was drawn from. It must not resolve to a play with no item.
     */
    @Test
    fun aTapWithNothingToActOnDoesNothing() = runTest {
        val port = FakePlaybackPort()
        val viewModel = NowPlayingViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback()
        advanceUntilIdle()

        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)
    }

    private fun playing(summary: PlaybackSummary) = FakePlaybackPort().apply { publish(summary) }

    /** `WhileSubscribed` publishes nothing until something is listening. */
    private fun TestScope.collectState(viewModel: NowPlayingViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

    private companion object {
        val RAIN = PlaybackItemId.sound("gentle-rain")
        val WAVES = PlaybackItemId.sound("calm-waves")
    }
}
