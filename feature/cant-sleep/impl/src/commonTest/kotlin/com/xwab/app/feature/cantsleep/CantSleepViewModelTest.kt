package com.xwab.app.feature.cantsleep

import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuidePort
import com.xwab.app.core.guide.port.GuideUse
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.feature.cantsleep.domain.ObserveCantSleepContentUseCase
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
class CantSleepViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** The wind-down guides belong to After Shift; this screen lists only what is heard out of bed. */
    @Test
    fun onlyOutOfBedGuidesAreListed() = runTest(dispatcher) {
        val model = model(FakePlaybackPort())
        collectState(model)
        advanceUntilIdle()

        assertEquals(listOf(AWAKE), ready(model).guides.map { it.id })
    }

    @Test
    fun aGuidePlaysThenPausesFromItsOwnControl() = runTest(dispatcher) {
        val port = FakePlaybackPort()
        val model = model(port)
        collectState(model)
        advanceUntilIdle()

        model.togglePlayback(AWAKE)
        advanceUntilIdle()
        assertEquals(ITEM, port.playedItemId)

        port.publish(PlaybackSummary(requestedItemId = ITEM, playIntent = true, isPreparing = true))
        advanceUntilIdle()
        assertTrue(ready(model).isGuidePlaying(AWAKE))
        assertTrue(ready(model).isGuidePreparing(AWAKE))
        model.togglePlayback(AWAKE)
        assertEquals(1, port.pauses)
    }

    /** A wind-down guide the session is on, and its failure, are not this screen's. */
    @Test
    fun anotherScreensGuideIsNotThisScreens() = runTest(dispatcher) {
        val relax = PlaybackItemId(GUIDE_PLAYBACK_KIND, "relax")
        val port = FakePlaybackPort().apply { publish(PlaybackSummary(requestedItemId = relax, playIntent = true)) }
        val model = model(port)
        collectState(model)
        advanceUntilIdle()
        assertNull(ready(model).requestedGuideId)
        assertFalse(ready(model).playIntent)

        port.publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(relax)))
        advanceUntilIdle()
        assertNull(ready(model).playbackFailure)

        port.publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(ITEM)))
        advanceUntilIdle()
        assertIs<PlaybackFailure.SourceUnavailable>(ready(model).guideFailure(AWAKE))
    }

    private fun model(port: FakePlaybackPort) = CantSleepViewModel(
        ObserveCantSleepContentUseCase(
            FakeGuides(listOf(guide("relax", GuideUse.WindDown), guide("awake", GuideUse.OutOfBed))),
            port,
        ),
        port,
    )

    private fun TestScope.collectState(model: CantSleepViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
    }

    private fun ready(model: CantSleepViewModel) = assertIs<CantSleepUiState.Ready>(model.state.value).value

    private companion object {
        val AWAKE = GuideId("awake")
        val ITEM = PlaybackItemId(GUIDE_PLAYBACK_KIND, "awake")
    }
}

private fun guide(id: String, use: GuideUse) =
    Guide(GuideId(id), id, "A test guide.", minutes = 5, languageTag = "en-US", use = use)

/** One feature reads this port this way, so its fake lives with it rather than in `testing`. */
private class FakeGuides(private val guides: List<Guide>) : GuidePort {
    override fun observeGuides(): Flow<List<Guide>> = flowOf(guides)
    override fun observeGuide(guideId: GuideId): Flow<Guide?> = flowOf(guides.find { it.id == guideId })
}
