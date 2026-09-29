package com.xwab.app.feature.favorites

import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.feature.favorites.domain.ObserveFavoritesContentUseCase
import com.xwab.app.testing.FakeFavorites
import com.xwab.app.testing.FakePlaybackPort
import com.xwab.app.testing.FakeSoundCatalog
import com.xwab.app.testing.track
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRemovalTest {
    private lateinit var mainDispatcher: TestDispatcher
    private val rain = track("rain")
    private val ocean = track("ocean")

    @BeforeTest
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun removingAFavoritePublishesUndoWithoutStoppingPlayback() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val playback = FakePlaybackPort()
        val viewModel = show(favorites, playback)

        viewModel.removeFavorite(rain.id)
        runCurrent()

        assertTrue(ready(viewModel).tracks.isEmpty())
        assertEquals(rain, viewModel.removedTrack.value)
        assertEquals(listOf(Triple(SOUND_FAVORITES_NAMESPACE, rain.id.value, false)), favorites.sets)
        assertTrue(favorites.toggles.isEmpty())
        assertFalse(ready(viewModel).favoriteWriteFailed)
        assertEquals(0, playback.pauses)
        assertNull(playback.playedItemId)
    }

    @Test
    fun repeatedRemoveTapsWhileStorageIsBusyNeverAddTheItemBack() = runTest(mainDispatcher) {
        val backing = FakeFavorites(setOf(rain.id))
        val writeStarted = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        val favorites = object : FavoritesPort by backing {
            override suspend fun setFavorite(
                namespace: String,
                itemId: String,
                isFavorite: Boolean,
            ): FavoriteToggleResult {
                writeStarted.complete(Unit)
                releaseWrite.await()
                return backing.setFavorite(namespace, itemId, isFavorite)
            }
        }
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()
        assertTrue(writeStarted.isCompleted)

        // The row is still visible until the first write finishes.
        viewModel.removeFavorite(rain.id)
        runCurrent()
        releaseWrite.complete(Unit)
        runCurrent()

        assertEquals(listOf(false, false), backing.sets.map { it.third })
        assertTrue(backing.toggles.isEmpty())
        assertTrue(ready(viewModel).tracks.isEmpty())
        assertEquals(rain, viewModel.removedTrack.value)
    }

    @Test
    fun repeatedUndoRestoresTheFavoriteExactlyOnce() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()

        viewModel.undoRemoval(rain.id)
        viewModel.undoRemoval(rain.id)
        runCurrent()

        assertEquals(listOf(rain), ready(viewModel).tracks)
        assertEquals(listOf(false, true), favorites.sets.map { it.third })
        assertNull(viewModel.removedTrack.value)
        assertFalse(ready(viewModel).favoriteWriteFailed)
    }

    @Test
    fun failedRemovalKeepsTheRowAndSuccessfulRetryClearsTheError() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id)).apply {
            toggleResult = FavoriteToggleResult.Unavailable
        }
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()

        assertEquals(listOf(rain), ready(viewModel).tracks)
        assertNull(viewModel.removedTrack.value)
        assertTrue(ready(viewModel).favoriteWriteFailed)

        favorites.toggleResult = FavoriteToggleResult.Updated
        viewModel.removeFavorite(rain.id)
        runCurrent()
        assertTrue(ready(viewModel).tracks.isEmpty())
        assertFalse(ready(viewModel).favoriteWriteFailed)
        assertEquals(rain, viewModel.removedTrack.value)
    }

    @Test
    fun failedUndoKeepsThePendingRemovalSoItCanBeRetried() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()
        favorites.toggleResult = FavoriteToggleResult.Unavailable

        viewModel.undoRemoval(rain.id)
        runCurrent()

        assertTrue(ready(viewModel).tracks.isEmpty())
        assertTrue(ready(viewModel).favoriteWriteFailed)
        assertEquals(rain, viewModel.removedTrack.value)
        assertTrue(viewModel.undoFailed.value)

        favorites.toggleResult = FavoriteToggleResult.Updated
        viewModel.undoRemoval(rain.id)
        runCurrent()
        assertEquals(listOf(rain), ready(viewModel).tracks)
        assertFalse(ready(viewModel).favoriteWriteFailed)
        assertNull(viewModel.removedTrack.value)
        assertFalse(viewModel.undoFailed.value)
    }

    @Test
    fun unavailableFavoritesCannotBeRemovedFromRetainedRows() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites)
        favorites.available.value = false
        runCurrent()

        viewModel.removeFavorite(rain.id)
        runCurrent()

        assertEquals(listOf(rain), ready(viewModel).tracks)
        assertFalse(ready(viewModel).favoritesAvailable)
        assertTrue(favorites.toggles.isEmpty())
        assertTrue(favorites.sets.isEmpty())
        assertNull(viewModel.removedTrack.value)
    }

    @Test
    fun anExpiredOrOlderUndoCannotRestoreADifferentRemoval() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id, ocean.id))
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()
        viewModel.removeFavorite(ocean.id)
        runCurrent()

        viewModel.acknowledgeRemoval(rain.id)
        viewModel.undoRemoval(rain.id)
        runCurrent()
        assertEquals(ocean, viewModel.removedTrack.value)
        assertTrue(favorites.observe(SOUND_FAVORITES_NAMESPACE).first().ids.isEmpty())

        viewModel.acknowledgeRemoval(ocean.id)
        viewModel.undoRemoval(ocean.id)
        runCurrent()
        assertNull(viewModel.removedTrack.value)
        assertEquals(2, favorites.sets.size)
    }

    @Test
    fun undoDoesNotToggleOffAnItemAlreadyRestoredElsewhere() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()
        favorites.toggle(SOUND_FAVORITES_NAMESPACE, rain.id.value)
        runCurrent()

        viewModel.undoRemoval(rain.id)
        runCurrent()

        assertEquals(listOf(rain), ready(viewModel).tracks)
        assertEquals(listOf(false, true), favorites.sets.map { it.third })
        assertEquals(1, favorites.toggles.size)
        assertNull(viewModel.removedTrack.value)
    }

    @Test
    fun anotherScreenRemovingDuringOurWriteCannotInvertTheRemoval() = runTest(mainDispatcher) {
        val backing = FakeFavorites(setOf(rain.id))
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val favorites = object : FavoritesPort by backing {
            override suspend fun setFavorite(
                namespace: String,
                itemId: String,
                isFavorite: Boolean,
            ): FavoriteToggleResult {
                entered.complete(Unit)
                finish.await()
                return backing.setFavorite(namespace, itemId, isFavorite)
            }
        }
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()
        assertTrue(entered.isCompleted)
        backing.toggle(SOUND_FAVORITES_NAMESPACE, rain.id.value)
        finish.complete(Unit)
        runCurrent()

        assertTrue(ready(viewModel).tracks.isEmpty())
        assertEquals(listOf(false), backing.sets.map { it.third })
        assertEquals(rain, viewModel.removedTrack.value)
    }

    @Test
    fun anotherScreenRestoringDuringUndoCannotInvertTheRestore() = runTest(mainDispatcher) {
        val backing = FakeFavorites(setOf(rain.id))
        val undoEntered = CompletableDeferred<Unit>()
        val finishUndo = CompletableDeferred<Unit>()
        val favorites = object : FavoritesPort by backing {
            override suspend fun setFavorite(
                namespace: String,
                itemId: String,
                isFavorite: Boolean,
            ): FavoriteToggleResult {
                if (isFavorite) {
                    undoEntered.complete(Unit)
                    finishUndo.await()
                }
                return backing.setFavorite(namespace, itemId, isFavorite)
            }
        }
        val viewModel = show(favorites)
        viewModel.removeFavorite(rain.id)
        runCurrent()
        viewModel.undoRemoval(rain.id)
        runCurrent()
        assertTrue(undoEntered.isCompleted)
        backing.toggle(SOUND_FAVORITES_NAMESPACE, rain.id.value)
        finishUndo.complete(Unit)
        runCurrent()

        assertEquals(listOf(rain), ready(viewModel).tracks)
        assertEquals(listOf(false, true), backing.sets.map { it.third })
        assertNull(viewModel.removedTrack.value)
    }

    /**
     * Leaving the list cancels its snackbar without an answer. Coming back within the window offers
     * Undo again; later, the offer is dropped rather than resurfacing long after the removal.
     */
    @Test
    fun theUndoOfferIsRepeatedOnlyShortlyAfterTheRemoval() = runTest(mainDispatcher) {
        val clock = TestTimeSource()
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites, timeSource = clock)
        viewModel.removeFavorite(rain.id)
        runCurrent()

        assertTrue(viewModel.claimUndoOffer(rain.id))
        clock += 5.seconds
        assertTrue(viewModel.claimUndoOffer(rain.id), "back on screen within the window")
        clock += 6.seconds
        assertFalse(viewModel.claimUndoOffer(rain.id), "back on screen after the window")
        assertNull(viewModel.removedTrack.value)
        assertTrue(ready(viewModel).tracks.isEmpty())
    }

    @Test
    fun aSoundSavedAgainElsewhereIsNotOfferedForUndo() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites, timeSource = TestTimeSource())
        viewModel.removeFavorite(rain.id)
        runCurrent()

        favorites.setFavorite(SOUND_FAVORITES_NAMESPACE, rain.id.value, true)
        runCurrent()

        assertFalse(viewModel.claimUndoOffer(rain.id))
        assertNull(viewModel.removedTrack.value)
    }

    @Test
    fun aFailedUndoKeepsItsRetryInsteadOfOfferingUndoAgain() = runTest(mainDispatcher) {
        val favorites = FakeFavorites(setOf(rain.id))
        val viewModel = show(favorites, timeSource = TestTimeSource())
        viewModel.removeFavorite(rain.id)
        runCurrent()
        favorites.toggleResult = FavoriteToggleResult.Unavailable
        viewModel.undoRemoval(rain.id)
        runCurrent()

        assertFalse(viewModel.claimUndoOffer(rain.id))
        assertEquals(rain, viewModel.removedTrack.value, "the retry control still needs the removal")
        assertTrue(viewModel.undoFailed.value)
    }

    private fun TestScope.show(
        favorites: FavoritesPort,
        playback: FakePlaybackPort = FakePlaybackPort(),
        timeSource: TimeSource = TimeSource.Monotonic,
    ): FavoritesViewModel {
        val viewModel = FavoritesViewModel(
            ObserveFavoritesContentUseCase(
                FakeSoundCatalog(tracks = listOf(rain, ocean)), favorites, playback,
            ),
            playback,
            favorites,
            timeSource,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        runCurrent()
        assertIs<FavoritesUiState.Ready>(viewModel.state.value)
        return viewModel
    }

    private fun ready(viewModel: FavoritesViewModel): FavoritesState =
        assertIs<FavoritesUiState.Ready>(viewModel.state.value).value
}
