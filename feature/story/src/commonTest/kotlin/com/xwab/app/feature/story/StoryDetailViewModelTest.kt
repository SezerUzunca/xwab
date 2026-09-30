package com.xwab.app.feature.story

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.domain.ObserveStoryContentUseCase
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
class StoryDetailViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun openingAnotherStoryDoesNotChangeThePlayingItem() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = PlaybackItemId(STORY_PLAYBACK_KIND, "other"), playIntent = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertEquals("bedtime", ready(model).story?.title)
        assertFalse(ready(model).playIntent)
        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)

        model.togglePlayback()
        advanceUntilIdle()
        assertEquals(ITEM, port.playedItemId)
        assertEquals(0, port.pauses)
    }

    @Test
    fun preparingStoryCanBePausedAndOnlyItsOwnFailureIsShown() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true, isPreparing = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertTrue(ready(model).isPreparing)
        model.togglePlayback()
        assertEquals(1, port.pauses)

        val failure = PlaybackFailure.SourceUnavailable(ITEM)
        port.publish(PlaybackSummary(failure = failure))
        advanceUntilIdle()
        assertEquals(failure, ready(model).failure)
        port.publish(PlaybackSummary(failure = PlaybackFailure.EngineFailed(PlaybackItemId("other-kind", "bedtime"))))
        advanceUntilIdle()
        assertNull(ready(model).failure)
    }

    @Test
    fun missingStoryCannotStartButCanPauseIfAlreadyRequested() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port, missing = true)
        collectState(model)
        advanceUntilIdle()
        assertNull(ready(model).story)
        assertFalse(ready(model).canPlay)
        model.togglePlayback()
        advanceUntilIdle()
        assertNull(port.playedItemId)
        port.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true))
        advanceUntilIdle()
        assertTrue(ready(model).canPlay)
        model.togglePlayback()
        assertEquals(1, port.pauses)
    }

    /** Repeat and the timer reach the session unchanged, and the switch shows the session's value. */
    @Test
    fun sessionSettingsReachTheSessionWithoutStartingTheStory() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply { publish(PlaybackSummary(isLooping = true)) }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertTrue(ready(model).isLooping)

        model.setLooping(false)
        model.startSleepTimer(-1L)
        model.startSleepTimer(600_000L)
        model.cancelSleepTimer()
        advanceUntilIdle()

        assertEquals(false, port.looping)
        assertEquals(600_000L, port.startedTimerMs)
        assertEquals(1, port.cancelledTimers)
        assertNull(port.playedItemId)
    }

    private fun model(port: FakePlaybackPort, missing: Boolean = false): StoryDetailViewModel =
        StoryDetailViewModel(
            StoryId("bedtime"),
            ObserveStoryContentUseCase(FakeStoryCatalog(if (missing) emptyList() else listOf(story("bedtime"))), port),
            port,
        )

    private fun TestScope.collectState(model: StoryDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect() }
    }

    private fun ready(model: StoryDetailViewModel): StoryDetailState =
        assertIs<StoryDetailUiState.Ready>(model.state.value).value

    private companion object {
        val ITEM = PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime")
    }
}
