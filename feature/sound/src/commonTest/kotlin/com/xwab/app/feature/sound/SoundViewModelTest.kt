package com.xwab.app.feature.sound

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.sound.domain.ObserveSoundContentUseCase
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoritesSnapshot
import com.xwab.app.feature.sound.domain.SoundFavoriteReadStatus
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.track
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * This screen holds the most behaviour of any — the session's failure taxonomy translated
 * into this screen's own, the sleep timer refusing to start on a track that does not exist, and the
 * controls that pass straight through. Only the use case behind it was covered before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SoundViewModelTest {
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

        assertEquals(SoundError.SoundUnavailable, readyState(viewModel).error)
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
        assertNull(state.track)
        assertEquals(SoundError.SoundNotFound, state.error)
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

    /**
     * The favorites namespace a sound is stored under is shared by three screens, and a screen that
     * named a different one would read an empty store rather than fail to build. The value itself
     * is pinned by `SoundFavoritesTest`; what this pins is that the write and the read that
     * follows it agree on it.
     */
    @Test
    fun togglingAFavoriteWritesAndReadsBackUnderTheSoundNamespace() = runTest(mainDispatcher) {
        val favorites = FakeFavorites()
        val viewModel = createViewModel(FakePlaybackPort(), favorites = favorites)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(listOf(SOUND_FAVORITES_NAMESPACE to RAIN.value), favorites.toggles)
        assertTrue(readyState(viewModel).isFavorite)
    }

    /**
     * The panel draws all three controls as disabled without a track. The ViewModel used to refuse
     * only the sleep timer, so the same rule held in one layer and not the other.
     */
    @Test
    fun noControlActsOnATrackThatDoesNotExist() = runTest(mainDispatcher) {
        val port = FakePlaybackPort()
        val viewModel = createViewModel(port, catalogHasTrack = false)
        collectState(viewModel)
        advanceUntilIdle()

        viewModel.setLooping(false)
        viewModel.setVolume(0.4f)
        viewModel.startSleepTimer(FIFTEEN_MINUTES_MS)

        assertNull(port.looping)
        assertNull(port.volume)
        assertNull(port.startedTimerMs)
    }

    /**
     * A sound the catalog does not hold offers nothing to do, and both layers now say so from the
     * same predicate. Play used to be refused by neither: the button looked live and asked the
     * session for a sound nothing could resolve, which took the session's claim off whatever was
     * playing to fail on this one.
     */
    @Test
    fun nothingCanBeDoneToATrackTheCatalogDoesNotHold() = runTest(mainDispatcher) {
        val port = FakePlaybackPort()
        val viewModel = createViewModel(port, catalogHasTrack = false)
        collectState(viewModel)
        advanceUntilIdle()

        val state = readyState(viewModel)
        assertFalse(state.canPlay)
        assertFalse(state.canFavorite)
        assertFalse(state.canConfigure)

        viewModel.togglePlayback()
        advanceUntilIdle()

        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)
    }

    /**
     * Unless the session is already on it. A sound leaving the catalog while it plays must not
     * leave audible sound with nothing able to stop it — the same reason cancelling a running
     * sleep timer is never refused.
     */
    @Test
    fun aSoundAlreadyPlayingCanStillBePausedAfterItLeavesTheCatalog() = runTest(mainDispatcher) {
        val port = FakePlaybackPort().apply {
            publish(PlaybackSummary(requestedItemId = RAIN_ITEM, playIntent = true))
        }
        val viewModel = createViewModel(port, catalogHasTrack = false)
        collectState(viewModel)
        advanceUntilIdle()

        assertTrue(readyState(viewModel).canPlay)

        viewModel.togglePlayback()

        assertEquals(1, port.pauses)
    }

    @Test
    fun favoriteReadFailuresAreVisibleAndRecoverWithoutReopeningTheScreen() = runTest(mainDispatcher) {
        val favorites = FakeFavorites()
        val viewModel = createViewModel(FakePlaybackPort(), favorites = favorites)
        collectState(viewModel)
        advanceUntilIdle()
        favorites.available.value = false
        advanceUntilIdle()
        assertFalse(readyState(viewModel).favoritesAvailable)
        favorites.available.value = true
        advanceUntilIdle()
        assertTrue(readyState(viewModel).favoritesAvailable)
    }

    @Test
    fun favoriteWriteFailuresAreVisibleUntilASuccessfulRetry() = runTest(mainDispatcher) {
        val favorites = FakeFavorites().apply { toggleResult = FavoriteToggleResult.Unavailable }
        val viewModel = createViewModel(FakePlaybackPort(), favorites = favorites)
        collectState(viewModel)
        advanceUntilIdle()
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(readyState(viewModel).favoriteWriteFailed)
        favorites.toggleResult = FavoriteToggleResult.Updated
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertFalse(readyState(viewModel).favoriteWriteFailed)
    }
    @Test
    fun contentAndPlaybackAreUsableBeforeTheFirstFavoriteRead() = runTest(mainDispatcher) {
        val reads = MutableSharedFlow<FavoritesSnapshot>(replay = 1)
        val writes = FakeFavorites()
        val playback = FakePlaybackPort()
        val viewModel = createViewModel(playback, favorites = favoritesReading(reads, writes))
        collectState(viewModel)
        runCurrent()

        val pending = readyState(viewModel)
        assertEquals(RAIN, pending.track?.id)
        assertEquals(SoundFavoriteReadStatus.Pending, pending.favoriteReadStatus)
        assertTrue(pending.canPlay)
        assertTrue(pending.canConfigure)
        assertFalse(pending.canFavorite)
        viewModel.toggleFavorite()
        viewModel.togglePlayback()
        runCurrent()
        assertTrue(writes.toggles.isEmpty())
        assertEquals(RAIN_ITEM, playback.playedItemId)

        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        runCurrent()
        assertEquals(SoundFavoriteReadStatus.Unavailable, readyState(viewModel).favoriteReadStatus)
        assertFalse(readyState(viewModel).canFavorite)

        reads.emit(FavoritesSnapshot(setOf(RAIN.value)))
        runCurrent()
        assertTrue(readyState(viewModel).isFavorite)
        assertTrue(readyState(viewModel).canFavorite)
    }

    @Test
    fun favoriteMembershipSurvivesAStoppedSubscriptionAndFailedFirstRead() = runTest(mainDispatcher) {
        val reads = MutableSharedFlow<FavoritesSnapshot>(replay = 1)
        reads.emit(FavoritesSnapshot(setOf(RAIN.value)))
        val viewModel = createViewModel(FakePlaybackPort(), favorites = favoritesReading(reads))
        val firstCollection = collectState(viewModel)
        runCurrent()
        assertTrue(readyState(viewModel).isFavorite)

        firstCollection.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, reads.subscriptionCount.value)
        reads.resetReplayCache()
        val secondCollection = collectState(viewModel)
        runCurrent()
        assertEquals(SoundFavoriteReadStatus.Pending, readyState(viewModel).favoriteReadStatus)
        assertTrue(readyState(viewModel).isFavorite)

        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        runCurrent()
        assertEquals(SoundFavoriteReadStatus.Unavailable, readyState(viewModel).favoriteReadStatus)
        assertTrue(readyState(viewModel).isFavorite)
        assertFalse(readyState(viewModel).canFavorite)

        reads.emit(FavoritesSnapshot(emptySet()))
        runCurrent()
        assertFalse(readyState(viewModel).isFavorite)
        assertTrue(readyState(viewModel).canFavorite)

        secondCollection.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        reads.emit(FavoritesSnapshot(emptySet(), isAvailable = false))
        collectState(viewModel)
        runCurrent()
        assertFalse(readyState(viewModel).isFavorite)
        assertFalse(readyState(viewModel).favoritesAvailable)
    }

    private fun favoritesReading(reads: Flow<FavoritesSnapshot>, writes: FakeFavorites = FakeFavorites()): FavoritesPort =
        object : FavoritesPort by writes {
            override fun observe(namespace: String): Flow<FavoritesSnapshot> = reads
        }

    private fun createViewModel(
        port: FakePlaybackPort,
        catalogHasTrack: Boolean = true,
        favorites: FavoritesPort = FakeFavorites(setOf(RAIN)),
    ): SoundViewModel {
        val catalog = FakeSoundCatalog(
            tracks = if (catalogHasTrack) listOf(track(RAIN.value, categoryId = "rain")) else emptyList(),
        )
        val useCase = ObserveSoundContentUseCase(
            soundPort = catalog,
            favoritesPort = favorites,
            playbackPort = port,
        )
        // One store rather than two, which is the wiring the graph builds: what the ViewModel
        // writes is what the use case reads back. Two instances made the round trip untestable,
        // and with it the namespace the write goes under.
        return SoundViewModel(RAIN, useCase, favorites, port)
    }

    private fun readyState(viewModel: SoundViewModel): SoundState =
        assertIs<SoundUiState.Ready>(viewModel.state.value).value

    private fun TestScope.collectState(viewModel: SoundViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }

    private companion object {
        val RAIN = TrackId("gentle-rain")
        val RAIN_ITEM = PlaybackItemId.sound("gentle-rain")
        const val FIFTEEN_MINUTES_MS = 15L * 60_000L
    }
}
