package com.xwab.app.feature.shuffle

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.core.shuffle.port.ShufflePort
import com.xwab.app.feature.shuffle.domain.ObserveShuffleContentUseCase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class ShuffleViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aDeckPlaysThenPausesFromItsOwnControl() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.togglePlayback(EVERYDAY)
        advanceUntilIdle()
        assertEquals(PlaybackItemId(SHUFFLE_PLAYBACK_KIND, "everyday"), port.playedItemId)

        port.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true))
        advanceUntilIdle()
        assertTrue(ready(model).isDeckPlaying(EVERYDAY))
        model.togglePlayback(EVERYDAY)
        assertEquals(1, port.pauses)
    }

    /** A sound or a story playing is no deck playing: the control offers play, not pause. */
    @Test
    fun anotherKindPlayingLeavesEveryDeckIdle() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = PlaybackItemId("sound", "everyday"), playIntent = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        assertFalse(ready(model).isAnyDeckPlaying)
        assertNull(ready(model).requestedDeckId)
        model.togglePlayback(EVERYDAY)
        advanceUntilIdle()
        assertEquals(0, port.pauses)
        assertEquals(ITEM, port.playedItemId)
    }

    /** The words take a moment to speak the first time; the deck says so, and only its own failure shows. */
    @Test
    fun aPreparingDeckSaysSoAndShowsOnlyItsOwnFailure() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true, isPreparing = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertTrue(ready(model).isDeckPreparing(EVERYDAY))

        port.publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(ITEM)))
        advanceUntilIdle()
        assertIs<PlaybackFailure.SourceUnavailable>(ready(model).deckFailure(EVERYDAY))
        assertNull(ready(model).deckFailure(ShuffleDeckId("other")))

        port.publish(PlaybackSummary(failure = PlaybackFailure.EngineFailed(PlaybackItemId("story", "everyday"))))
        advanceUntilIdle()
        assertNull(ready(model).deckFailure(EVERYDAY), "a story's failure is not this deck's")
    }

    /** A timer set beside the words is for the words: it starts the first deck when none plays. */
    @Test
    fun startingTheTimerPlaysTheFirstDeckWhenNoneIsPlaying() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.startSleepTimer(-1L)
        assertNull(port.startedTimerMs, "an invalid duration starts nothing")
        model.startSleepTimer(1_200_000L)
        advanceUntilIdle()

        assertEquals(1_200_000L, port.startedTimerMs)
        assertEquals(ITEM, port.playedItemId)
        model.cancelSleepTimer()
        assertEquals(1, port.cancelledTimers)
    }

    @Test
    fun restartingTheTimerWhileADeckPlaysLeavesPlaybackAlone() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = playbackItemOf(ShuffleDeckId("other")), playIntent = true))
        }
        val model = model(port, decks = listOf(deck("everyday"), deck("other")))
        collectState(model)
        advanceUntilIdle()

        model.startSleepTimer(600_000L)
        advanceUntilIdle()

        assertEquals(600_000L, port.startedTimerMs)
        assertNull(port.playedItemId)
    }

    private fun model(port: FakePlaybackPort, decks: List<ShuffleDeck> = listOf(deck("everyday"))) =
        ShuffleViewModel(ObserveShuffleContentUseCase(FakeShuffleCatalog(decks), port), port)

    private fun TestScope.collectState(model: ShuffleViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
    }

    private fun ready(model: ShuffleViewModel) = assertIs<ShuffleUiState.Ready>(model.state.value).value

    private companion object {
        val EVERYDAY = ShuffleDeckId("everyday")
        val ITEM = PlaybackItemId(SHUFFLE_PLAYBACK_KIND, "everyday")
    }
}

internal fun deck(id: String) = ShuffleDeck(
    id = ShuffleDeckId(id),
    title = id,
    description = "A test deck.",
    languageTag = "en-US",
    wordIntervalSeconds = 8,
)

/**
 * An in-memory deck catalog. Here rather than in `testing`, as the story fake is: one feature reads
 * this port, and a shared fake would put `core:shuffle` on every feature's test classpath.
 */
internal class FakeShuffleCatalog(private val decks: List<ShuffleDeck>) : ShufflePort {
    override fun observeDecks(): Flow<List<ShuffleDeck>> = flowOf(decks)

    override fun observeDeck(deckId: ShuffleDeckId): Flow<ShuffleDeck?> = flowOf(decks.find { it.id == deckId })
}
