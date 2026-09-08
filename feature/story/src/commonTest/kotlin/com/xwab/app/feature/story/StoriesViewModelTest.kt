package com.xwab.app.feature.story

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.designsystem.state.Loadable
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
                    requestedItemId = PlaybackItemId.sound("bedtime"),
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
                    failure = PlaybackFailure.SourceUnavailable(PlaybackItemId.sound("bedtime")),
                ),
            )
        }
        val ignoring = createViewModel(soundFailed)
        collectState(ignoring)
        advanceUntilIdle()

        assertNull(readyState(ignoring).playbackFailure)

        val failure = PlaybackFailure.SourceUnavailable(PlaybackItemId.story("bedtime"))
        val storyFailed = FakePlaybackPort().apply { publish(PlaybackSummary(failure = failure)) }
        val showing = createViewModel(storyFailed)
        collectState(showing)
        advanceUntilIdle()

        assertEquals(failure, readyState(showing).playbackFailure)
    }

    @Test
    fun tappingTheRequestedPlayingStoryPauses() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(
                PlaybackSummary(
                    requestedItemId = PlaybackItemId.story("bedtime"),
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
                    requestedItemId = PlaybackItemId.story("bedtime"),
                    playIntent = true,
                ),
            )
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.togglePlayback(MOONLIGHT)
        advanceUntilIdle()

        assertEquals(PlaybackItemId.story("moonlight"), port.playedItemId)
        assertEquals(0, port.pauses)
    }

    private fun createViewModel(port: FakePlaybackPort): StoriesViewModel {
        val useCase = ObserveStoriesContentUseCase(
            storyCatalogPort = FakeStoryCatalog(listOf(story("bedtime"), story("moonlight"))),
            playbackPort = port,
        )
        return StoriesViewModel(useCase, port)
    }

    private fun readyState(viewModel: StoriesViewModel): StoriesState =
        assertIs<Loadable.Ready<StoriesState>>(viewModel.state.value).value

    private fun TestScope.collectState(viewModel: StoriesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }

    private companion object {
        val BEDTIME = StoryId("bedtime")
        val MOONLIGHT = StoryId("moonlight")
    }
}
