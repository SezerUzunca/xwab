package com.xwab.app.feature.sounds

import com.xwab.app.core.playback.port.PlaybackFailure
import com.xwab.app.core.playback.port.PlaybackItemId
import com.xwab.app.core.playback.port.PlaybackSummary
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.feature.sounds.domain.ObservePlayerContentUseCase
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeMusicCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
 * The player holds the most behaviour of any screen — the session's failure taxonomy translated
 * into this screen's own, the sleep timer refusing to start on a track that does not exist, and the
 * controls that pass straight through. Only the use case behind it was covered before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun translatesTheSessionFailureIntoThisScreensOwnError() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(failure = PlaybackFailure.SourceUnavailable(RAIN_ITEM)))
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(PlayerError.AudioUnavailable, readyState(viewModel).error)
    }

    /**
     * A failure names the item it happened to, and the session has already fallen back to whatever
     * was playing before. Another sound's failure is not this screen's to report.
     */
    @Test
    fun ignoresAFailureThatBelongsToAnotherItem() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(failure = PlaybackFailure.EngineFailed(PlaybackItemId.sound("ocean"))))
        }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertNull(readyState(viewModel).error)
    }

    @Test
    fun aTrackTheCatalogDoesNotHoldReadsAsNotFound() = runTest(mainDispatcher) {
        val viewModel = createViewModel(FakePlaybackPort(), catalogHasTrack = false)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertNull(state.music)
        assertEquals(PlayerError.AudioNotFound, state.error)
    }

    @Test
    fun theSleepTimerOnlyStartsOnceThereIsATrackToStopPlaying() = runTest(mainDispatcher) {
        val withoutTrack = FakePlaybackPort()
        val missing = createViewModel(withoutTrack, catalogHasTrack = false)
        collectState(missing)
        advanceUntilIdle()

        missing.startSleepTimer(FIFTEEN_MINUTES_MS)
        assertNull(withoutTrack.startedTimerMs)

        val withTrack = FakePlaybackPort()
        val loaded = createViewModel(withTrack)
        collectState(loaded)
        advanceUntilIdle()

        loaded.startSleepTimer(FIFTEEN_MINUTES_MS)
        assertEquals(FIFTEEN_MINUTES_MS, withTrack.startedTimerMs)
    }

    /** Whatever the icon says, the tap does: both branches read the session's own intent. */
    @Test
    fun tappingBranchesOnTheIntentTheControlRenders() = runTest(mainDispatcher) {
        val playing = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = RAIN_ITEM, playIntent = true))
        }
        val pausing = createViewModel(playing)
        collectState(pausing)
        advanceUntilIdle()

        pausing.togglePlayback()
        assertEquals(1, playing.pauses)
        assertNull(playing.playedItemId)

        val idle = FakePlaybackPort()
        val starting = createViewModel(idle)
        collectState(starting)
        advanceUntilIdle()

        starting.togglePlayback()
        advanceUntilIdle()
        assertEquals(RAIN_ITEM, idle.playedItemId)
        assertEquals(0, idle.pauses)
    }

    @Test
    fun loopingAndVolumeReachTheSessionUnchanged() = runTest(mainDispatcher) {
        val port = FakePlaybackPort()
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.setLooping(false)
        viewModel.setVolume(0.4f)
        viewModel.cancelSleepTimer()

        assertEquals(false, port.looping)
        assertEquals(0.4f, port.volume)
        assertEquals(1, port.cancelledTimers)
    }

    /** The control renders `state.volume` directly, so the state is where the range is guaranteed. */
    @Test
    fun volumeReachesTheStateWithinItsRange() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply { publish(PlaybackSummary(volume = 1.4f)) }
        val viewModel = createViewModel(port)
        collectState(viewModel)
        advanceUntilIdle()

        assertEquals(1.0f, readyState(viewModel).volume)
    }

    private fun createViewModel(
        port: FakePlaybackPort,
        catalogHasTrack: Boolean = true,
    ): PlayerViewModel {
        val catalog = FakeMusicCatalog(
            tracks = if (catalogHasTrack) listOf(track(RAIN.value, categoryId = "rain")) else emptyList(),
        )
        val useCase = ObservePlayerContentUseCase(
            soundCatalogPort = catalog,
            favoritesPort = FakeFavorites(setOf(RAIN)),
            playbackPort = port,
        )
        return PlayerViewModel(RAIN, useCase, FakeFavorites(setOf(RAIN)), port)
    }

    private fun readyState(viewModel: PlayerViewModel): PlayerState =
        assertIs<Loadable.Ready<PlayerState>>(viewModel.state.value).value

    private fun TestScope.collectState(viewModel: PlayerViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
    }

    private companion object {
        val RAIN = TrackId("gentle-rain")
        val RAIN_ITEM = PlaybackItemId.sound("gentle-rain")
        const val FIFTEEN_MINUTES_MS = 15L * 60_000L
    }
}
