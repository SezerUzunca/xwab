package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.feature.nowplaying.domain.ObserveNowPlayingContentUseCase
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
        val viewModel = playerViewModel(FakePlaybackPort())
        collectState(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isIdle)
    }

    @Test
    fun theBarNamesWhatTheSessionIsOn() = runTest {
        val port = playing(
            PlaybackSummary(requestedItemId = RAIN, title = "Gentle Rain", playIntent = true),
        )
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = viewModel.state.value
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

        val state = viewModel.state.value
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

        assertEquals(PlaybackFailure.SourceUnavailable(RAIN), viewModel.state.value.failure)
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

        assertNull(viewModel.state.value.failure)
    }

    @Test
    fun globalControlsOperateOnAnyContentKindWithoutOpeningItsDetails() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, playIntent = true))
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.setVolume(0.35f)
        viewModel.setLooping(false)
        viewModel.startSleepTimer(900_000L)
        viewModel.cancelSleepTimer()

        assertEquals(0.35f, port.volume)
        assertEquals(false, port.looping)
        assertEquals(900_000L, port.startedTimerMs)
        assertEquals(1, port.cancelledTimers)
        assertNull(port.playedItemId, "changing settings must not restart the item")
        assertEquals(0, port.pauses)
    }

    @Test
    fun timerTicksAndCancellationReachThePlayerWithoutAPlaybackUpdate() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, title = "Rain", playIntent = true))
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        port.publishSleepTimer(900_000L)
        advanceUntilIdle()
        assertEquals(900_000L, viewModel.state.value.sleepTimerRemainingMs)
        port.publishSleepTimer(899_000L)
        advanceUntilIdle()
        assertEquals(899_000L, viewModel.state.value.sleepTimerRemainingMs)
        assertEquals(RAIN, viewModel.state.value.itemId)

        port.publishSleepTimer(null)
        advanceUntilIdle()
        assertNull(viewModel.state.value.sleepTimerRemainingMs)
        assertTrue(viewModel.state.value.playIntent)
    }

    @Test
    fun settingsStayVisibleWhenPlaybackIsPaused() = runTest {
        val port = playing(PlaybackSummary(
            requestedItemId = RAIN,
            playIntent = false,
            isLooping = false,
            volume = 0.4f,
        ))
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0.4f, state.volume)
        assertFalse(state.isLooping)
        assertFalse(state.playIntent)
    }

    /**
     * Volume and repeat describe what is playing, so an idle session refuses them. The timer is the
     * session's own and stops whatever starts next: it can be set before a sound is picked, and one
     * that outlived playback can still be cancelled.
     */
    @Test
    fun anIdleSessionTakesATimerButNotItemSettings() = runTest {
        val port = FakePlaybackPort().apply { publishSleepTimer(300_000L) }
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.setVolume(0.5f)
        viewModel.setLooping(true)
        viewModel.startSleepTimer(60_000L)
        viewModel.cancelSleepTimer()

        assertNull(port.volume)
        assertNull(port.looping)
        assertEquals(60_000L, port.startedTimerMs)
        assertEquals(1, port.cancelledTimers)
        assertNull(port.playedItemId, "setting a timer must not start anything")
    }

    /** The item's own screen already has its play/pause and timer; any other screen keeps the bar. */
    @Test
    fun theMiniPlayerStepsAsideOnlyForTheScreenOfTheItemItHolds() {
        val playing = NowPlayingState(itemId = RAIN, playIntent = true)

        assertFalse(playing.showsMiniPlayerBeside(RAIN))
        assertTrue(playing.showsMiniPlayerBeside(WAVES))
        assertTrue(playing.showsMiniPlayerBeside(null))

        val timerOnly = NowPlayingState(sleepTimerRemainingMs = 60_000L)
        assertTrue(timerOnly.showsMiniPlayerBeside(RAIN), "a timer with nothing requested keeps its bar")
        assertFalse(NowPlayingState().showsMiniPlayerBeside(null))
    }

    @Test
    fun aRunningTimerKeepsTheMiniPlayerWithNothingRequested() = runTest {
        val port = FakePlaybackPort()
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.showsMiniPlayer)

        port.publishSleepTimer(300_000L)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isIdle)
        assertTrue(viewModel.state.value.showsMiniPlayer)

        port.publishSleepTimer(null)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.showsMiniPlayer)
    }

    @Test
    fun invalidTimerAndVolumeInputsDoNotReachTheSession() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN))
        val viewModel = playerViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.startSleepTimer(0L)
        viewModel.startSleepTimer(-1L)
        viewModel.setVolume(Float.NaN)
        viewModel.setVolume(Float.POSITIVE_INFINITY)

        assertNull(port.startedTimerMs)
        assertNull(port.volume)
    }

    @Test
    fun theEntryAndMiniPlayerObserveTheSameSessionAndReopeningDoesNotRestartIt() = runTest {
        val port = playing(PlaybackSummary(requestedItemId = RAIN, title = "Rain", playIntent = true))
        val miniPlayer = playerViewModel(port)
        val player = playerViewModel(port)
        collectState(miniPlayer)
        val playerSubscription = collectState(player)
        advanceUntilIdle()
        assertEquals(miniPlayer.state.value, player.state.value)

        playerSubscription.cancel()
        port.publish(PlaybackSummary(requestedItemId = WAVES, title = "Waves", playIntent = false))
        port.publishSleepTimer(300_000L)
        val reopenedPlayer = playerViewModel(port)
        collectState(reopenedPlayer)
        advanceUntilIdle()

        assertEquals(WAVES, reopenedPlayer.state.value.itemId)
        assertEquals(miniPlayer.state.value, reopenedPlayer.state.value)
        assertEquals(300_000L, reopenedPlayer.state.value.sleepTimerRemainingMs)
        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)
    }

    private fun playing(summary: PlaybackSummary) = FakePlaybackPort().apply { publish(summary) }

    private fun playerViewModel(port: PlaybackPort) =
        NowPlayingViewModel(ObserveNowPlayingContentUseCase(port), port)

    /** `WhileSubscribed` publishes nothing until something is listening. */
    private fun TestScope.collectState(viewModel: NowPlayingViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

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
