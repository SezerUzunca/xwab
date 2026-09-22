package com.xwab.app.feature.story

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.story.port.Story
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase
import com.xwab.app.testing.FakePlaybackPort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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
 * The session is shared by the whole app, and this screen lists only stories. Everything below
 * turns on that: what it takes from the session, what it refuses, and what a tap on a row does.
 * The catalog join behind it is covered by the use case's own test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StoriesViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * The session being on a sound is the same to this screen as the session being on nothing —
     * even when that sound shares its raw id with a story, which is the case the kind exists for.
     */
    @Test
    fun aSoundInTheSessionLeavesEveryRowIdle() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId(OTHER_KIND, "bedtime"),
                    playIntent = true,
                    isPreparing = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertNull(state.requestedStoryId)
        assertFalse(state.playIntent)
        assertFalse(state.isPreparing)
    }

    /** Same rule for failures: a sound that could not play is not this screen's to report. */
    @Test
    fun onlyAStorysFailureReachesThisScreen() = runTest(mainDispatcher) {
        val soundFailed = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    failure = PlaybackFailure.SourceUnavailable(PlaybackItemId(OTHER_KIND, "bedtime")),
                ),
            )
        }
        val ignoring = createViewModel(soundFailed)
        collectState(ignoring)
        advanceUntilIdle()

        assertNull(readyState(ignoring).playbackFailure)

        val failure = PlaybackFailure.SourceUnavailable(PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"))
        val storyFailed = FakePlaybackPort().apply { publish(PlaybackSummary(failure = failure)) }
        val showing = createViewModel(storyFailed)
        collectState(showing)
        advanceUntilIdle()

        assertEquals(failure, readyState(showing).playbackFailure)
        // Which row wears it is the state's answer too, and only the row it happened to.
        assertEquals(failure, readyState(showing).rowFailure(BEDTIME))
        assertNull(readyState(showing).rowFailure(MOONLIGHT))
    }

    @Test
    fun tappingTheRequestedPlayingStoryPauses() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"),
                    playIntent = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(BEDTIME)

        assertEquals(1, port.pauses)
        assertNull(port.playedItemId)
    }

    /** A tap on a different row moves the session rather than pausing the row already playing. */
    @Test
    fun tappingAnotherRowRequestsItInstead() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"),
                    playIntent = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(MOONLIGHT)
        advanceUntilIdle()

        assertEquals(PlaybackItemId(STORY_PLAYBACK_KIND, "moonlight"), port.playedItemId)
        assertEquals(0, port.pauses)
    }

    @Test
    fun theTimerFollowsTheSessionEvenWhenASoundIsPlaying() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = PlaybackItemId(OTHER_KIND, "bedtime")))
            publishSleepTimer(60_000L)
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(60_000L, readyState(viewModel).sleepTimerRemainingMs)
        port.publishSleepTimer(59_000L)
        advanceUntilIdle()
        assertEquals(59_000L, readyState(viewModel).sleepTimerRemainingMs)
        port.publishSleepTimer(null)
        advanceUntilIdle()
        assertNull(readyState(viewModel).sleepTimerRemainingMs)
    }

    @Test
    fun timerCommandsReachTheSessionWithoutChangingPlaybackOrLooping() = runTest(mainDispatcher) {
        val port = FakePlaybackPort()
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.startSleepTimer(15 * 60_000L)
        viewModel.cancelSleepTimer()

        assertEquals(15 * 60_000L, port.startedTimerMs)
        assertEquals(1, port.cancelledTimers)
        assertNull(port.playedItemId)
        assertNull(port.looping)
        assertEquals(0, port.pauses)
    }

    @Test
    fun anEmptyCatalogCannotStartATimerButCanStillCancelOne() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply { publishSleepTimer(60_000L) }
        val viewModel = createViewModel(port, stories = emptyList())
        collectState(viewModel)
        advanceUntilIdle()

        assertFalse(readyState(viewModel).canStartSleepTimer)
        assertEquals(60_000L, readyState(viewModel).sleepTimerRemainingMs)
        viewModel.startSleepTimer(15 * 60_000L)
        viewModel.cancelSleepTimer()

        assertNull(port.startedTimerMs)
        assertEquals(1, port.cancelledTimers)
    }

    @Test
    fun startingATimerWaitsForContentButCancellingDoesNot() = runTest(mainDispatcher) {
        val port = FakePlaybackPort()
        val viewModel = createViewModel(port)

        viewModel.startSleepTimer(15 * 60_000L)
        viewModel.cancelSleepTimer()

        assertIs<StoriesUiState.Loading>(viewModel.state.value)
        assertNull(port.startedTimerMs)
        assertEquals(1, port.cancelledTimers)
    }

    private fun createViewModel(
        port: FakePlaybackPort,
        stories: List<Story> = listOf(story("bedtime"), story("moonlight")),
    ): StoriesViewModel {
        val useCase = ObserveStoriesContentUseCase(
            storyPort = FakeStoryCatalog(stories),
            playbackPort = port,
        )
        return StoriesViewModel(useCase, port)
    }

    private fun readyState(viewModel: StoriesViewModel): StoriesState =
        assertIs<StoriesUiState.Ready>(viewModel.state.value).value

    private fun TestScope.collectState(viewModel: StoriesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }

    private companion object {
        val BEDTIME = StoryId("bedtime")
        val MOONLIGHT = StoryId("moonlight")
    }
}

/** Some kind this screen does not show, to prove it ignores one. */
private const val OTHER_KIND = "other-kind"
