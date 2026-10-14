package com.xwab.app.feature.nap

import com.xwab.app.core.nap.port.NAP_PLAYBACK_KIND
import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapId
import com.xwab.app.core.nap.port.NapPort
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.feature.nap.domain.ObserveNapContentUseCase
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
class NapViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** A timer running out before the wake-up would stop the nap before the voice comes. */
    @Test
    fun startingANapCancelsTheSleepTimerAndPausingLeavesItAlone() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.togglePlayback(SHORT)
        advanceUntilIdle()
        assertEquals(ITEM, port.playedItemId)
        assertEquals(1, port.cancelledTimers)

        port.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true))
        advanceUntilIdle()
        assertTrue(ready(model).isNapPlaying(SHORT))
        assertFalse(ready(model).isNapPlaying(LONG))
        model.togglePlayback(SHORT)
        assertEquals(1, port.pauses)
        assertEquals(1, port.cancelledTimers, "pausing does not touch the timer")
    }

    @Test
    fun aPreparingNapSaysSoAndShowsOnlyItsOwnFailure() = runTest(dispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true, isPreparing = true))
        }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertTrue(ready(model).isNapPreparing(SHORT))

        port.publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(ITEM)))
        advanceUntilIdle()
        assertIs<PlaybackFailure.SourceUnavailable>(ready(model).napFailure(SHORT))
        assertNull(ready(model).napFailure(LONG))

        port.publish(PlaybackSummary(failure = PlaybackFailure.EngineFailed(PlaybackItemId("guide", "nap-10"))))
        advanceUntilIdle()
        assertNull(ready(model).napFailure(SHORT), "another kind's failure is not this nap's")
    }

    private fun model(port: FakePlaybackPort) = NapViewModel(
        ObserveNapContentUseCase(FakeNaps(listOf(nap(SHORT, 10), nap(LONG, 20))), port),
        port,
    )

    private fun TestScope.collectState(model: NapViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
    }

    private fun ready(model: NapViewModel) = assertIs<NapUiState.Ready>(model.state.value).value

    private companion object {
        val SHORT = NapId("nap-10")
        val LONG = NapId("nap-20")
        val ITEM = PlaybackItemId(NAP_PLAYBACK_KIND, "nap-10")
    }
}

private fun nap(id: NapId, minutes: Int) = Nap(id, "$minutes-Minute Nap", "A test nap.", minutes, "en-US")

/** One feature reads this port, so its fake lives with it rather than in `testing`. */
private class FakeNaps(private val naps: List<Nap>) : NapPort {
    override fun observeNaps(): Flow<List<Nap>> = flowOf(naps)
    override fun observeNap(napId: NapId): Flow<Nap?> = flowOf(naps.find { it.id == napId })
}
