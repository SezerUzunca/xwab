package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.testing.FakePlaybackPort
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
class NowPlayingViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * Until the session answers, the bar does not know what is playing, so a tap in that gap has
     * nothing it could honestly act on.
     */
    @Test
    fun theBarWaitsForTheSessionsFirstAnswer() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, playIntent = true))
        val viewModel = playerViewModel(port)

        assertEquals(NowPlayingUiState.Loading, viewModel.state.value)
        viewModel.togglePlayback()
        advanceUntilIdle()

        assertEquals(0, port.pauses)
        assertNull(port.playedItemId)
    }

    @Test
    fun aSessionThatHasNeverBeenAskedForAnythingShowsNothing() = runTest {
        val viewModel = playerViewModel(FakePlaybackPort())
        collectState(viewModel)
        advanceUntilIdle()

        assertTrue(readyState(viewModel).isIdle)
    }

    @Test
    fun theBarNamesWhatTheSessionIsOn() = runTest {
        val port = playing(
            PlaybackSummary(requestedItemId = RAIN, title = "Gentle Rain", playIntent = true),
        )
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertFalse(state.isIdle)
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
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertFalse(state.isIdle, "the bar stays up across a switch")
        assertNull(state.title)
        assertTrue(state.isPreparing)
    }

    @Test
    fun aTapPausesWhatTheControlShowsAsPlaying() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, playIntent = true))
        val viewModel = playerViewModel(port)
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
        val viewModel = playerViewModel(port)
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
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback()
        advanceUntilIdle()

        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)
    }

    /**
     * The bar can start playback, so it owes an answer when that fails — it is the one control in
     * this app with no screen behind it to explain.
     */
    @Test
    fun aFailureOnThisItemIsReported() = runTest {
        val port = playing(
            PlaybackSummary(
                requestedItemId = RAIN,
                title = "Gentle Rain",
                failure = PlaybackFailure.SourceUnavailable(RAIN),
            ),
        )
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(PlaybackFailure.SourceUnavailable(RAIN), readyState(viewModel).failure)
    }

    /**
     * Another screen's row fails into the same session. A bar holding one item has nothing to say
     * about a different item's failure, and saying it would blame the wrong thing.
     */
    @Test
    fun aFailureOnSomethingElseIsNotThisBarsToReport() = runTest {
        val port = playing(
            PlaybackSummary(
                requestedItemId = RAIN,
                title = "Gentle Rain",
                failure = PlaybackFailure.SourceUnavailable(WAVES),
            ),
        )
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertNull(readyState(viewModel).failure)
    }

    @Test
    fun timerTicksReachTheBarWithoutAPlaybackUpdate() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, title = "Rain", playIntent = true))
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()
        val before = viewModel.state.value

        port.publishSleepTimer(900_000L)
        advanceUntilIdle()
        assertEquals(900_000L, viewModel.sleepTimerRemainingMs.value)
        port.publishSleepTimer(899_000L)
        advanceUntilIdle()
        assertEquals(899_000L, viewModel.sleepTimerRemainingMs.value)
        assertTrue(before === viewModel.state.value, "a timer tick must not publish a new bar state")

        port.publishSleepTimer(null)
        advanceUntilIdle()
        assertNull(viewModel.sleepTimerRemainingMs.value)
        assertTrue(readyState(viewModel).playIntent)
    }

    /**
     * A timer outliving playback stops whatever starts next, and with nothing requested there is no
     * item screen to cancel it from — so the bar does, without starting anything.
     */
    @Test
    fun aTimerWithNothingRequestedIsCancelledFromTheBar() = runTest {
        val port = FakePlaybackPort().apply { publishSleepTimer(300_000L) }
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()
        assertTrue(readyState(viewModel).isIdle)

        viewModel.cancelSleepTimer()

        assertEquals(1, port.cancelledTimers)
        assertNull(port.playedItemId, "cancelling a timer must not start anything")
    }

    /** An item's own screen already has its play/pause and timer card, so the bar gives way to it. */
    @Test
    fun theBarStepsAsideOnlyForTheScreenOfTheItemItHolds() {
        val playing = NowPlayingState(itemId = RAIN, playIntent = true)

        assertFalse(playing.showsBarBeside(RAIN, sleepTimerRemainingMs = null))
        assertTrue(playing.showsBarBeside(WAVES, sleepTimerRemainingMs = null))
        assertTrue(playing.showsBarBeside(null, sleepTimerRemainingMs = null))

        assertTrue(
            NowPlayingState().showsBarBeside(null, sleepTimerRemainingMs = 60_000L),
            "a timer with nothing requested keeps its bar",
        )
        assertFalse(
            NowPlayingState().showsBarBeside(RAIN, sleepTimerRemainingMs = 60_000L),
            "an item's screen already draws the timer and its cancel",
        )
        assertFalse(NowPlayingState().showsBarBeside(null, sleepTimerRemainingMs = null))
    }

    @Test
    fun aRunningTimerKeepsTheBarWithNothingRequested() = runTest {
        val port = FakePlaybackPort()
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()
        assertFalse(showsBar(viewModel))

        port.publishSleepTimer(300_000L)
        advanceUntilIdle()
        assertTrue(readyState(viewModel).isIdle)
        assertTrue(showsBar(viewModel))

        port.publishSleepTimer(null)
        advanceUntilIdle()
        assertFalse(showsBar(viewModel))
    }

    /** The session outlives the bar: a new bar picks up what is on without restarting it. */
    @Test
    fun aRecreatedBarShowsTheSessionWithoutRestartingIt() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, title = "Rain", playIntent = true))
        val subscription = collectState(playerViewModel(port))
        advanceUntilIdle()

        subscription.cancel()
        port.publish(PlaybackSummary(requestedItemId = WAVES, title = "Waves", playIntent = false))
        port.publishSleepTimer(300_000L)
        val recreated = playerViewModel(port)
        collectState(recreated)
        advanceUntilIdle()

        assertEquals(WAVES, readyState(recreated).itemId)
        assertEquals(300_000L, recreated.sleepTimerRemainingMs.value)
        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)
    }

    private fun playing(summary: PlaybackSummary) = FakePlaybackPort().apply { publish(summary) }

    private fun playerViewModel(port: PlaybackPort) = NowPlayingViewModel(playbackPort = port)

    private fun readyState(viewModel: NowPlayingViewModel): NowPlayingState =
        assertIs<NowPlayingUiState.Ready>(viewModel.state.value).value

    /** What the shell's bar decides, from the same two values it reads. */
    private fun showsBar(viewModel: NowPlayingViewModel): Boolean =
        readyState(viewModel).showsBarBeside(null, viewModel.sleepTimerRemainingMs.value)

    /** `WhileSubscribed` publishes nothing until something is listening; one job listens to both. */
    private fun TestScope.collectState(viewModel: NowPlayingViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            launch { viewModel.state.collect() }
            viewModel.sleepTimerRemainingMs.collect()
        }

    private companion object {
        val RAIN = PlaybackItemId(ANY_KIND, "gentle-rain")
        val WAVES = PlaybackItemId(ANY_KIND, "calm-waves")
    }
}

/**
 * This feature draws whatever is playing and never asks what kind it is, so its own fixtures
 * name a kind that belongs to no content module.
 */
private const val ANY_KIND = "any-kind"
