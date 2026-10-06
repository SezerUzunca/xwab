package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.xwab.app.core.favorites.port.FavoritesSnapshot
import com.xwab.app.core.favorites.port.FavoriteToggleResult

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreFavoritesAdapterTest {
    @Test
    fun explicitMembershipIsIdempotentAndKeepsOtherIdsAndNamespaces() = runTest {
        val store = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(store)
        adapter.setFavorite("sound", "rain", true)
        adapter.setFavorite("sound", "rain", true)
        adapter.setFavorite("sound", "ocean", true)
        adapter.setFavorite("story", "rain", true)
        assertEquals(setOf("rain", "ocean"), adapter.observe("sound").first().ids)

        adapter.setFavorite("sound", "rain", false)
        adapter.setFavorite("sound", "rain", false)
        val reopened = DataStoreFavoritesAdapter(store)
        assertEquals(setOf("ocean"), reopened.observe("sound").first().ids)
        assertEquals(setOf("rain"), reopened.observe("story").first().ids)
    }

    @Test
    fun concurrentRestoreRequestsFromSeparateAdaptersDoNotToggleTheItemOff() = runTest {
        val store = FakePreferencesDataStore()
        val firstScreen = DataStoreFavoritesAdapter(store)
        val secondScreen = DataStoreFavoritesAdapter(store)
        val entered = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        store.beforeNextWrite = {
            entered.complete(Unit)
            releaseWrite.await()
        }
        val first = launch { firstScreen.setFavorite("sound", "rain", true) }
        runCurrent()
        assertTrue(entered.isCompleted)
        val second = launch { secondScreen.setFavorite("sound", "rain", true) }
        runCurrent()
        releaseWrite.complete(Unit)
        first.join()
        second.join()

        assertEquals(setOf("rain"), firstScreen.observe("sound").first().ids)
    }

    @Test
    fun explicitMembershipValidatesInputsAndPreservesFailureAndCancellation() = runTest {
        val store = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(store)
        assertFailsWith<IllegalArgumentException> { adapter.setFavorite("../sound", "rain", true) }
        assertFailsWith<IllegalArgumentException> { adapter.setFavorite("sound", " ", false) }
        adapter.setFavorite("sound", "rain", true)

        store.writeFailure = IllegalStateException("disk full")
        assertEquals(FavoriteToggleResult.Unavailable, adapter.setFavorite("sound", "rain", false))
        store.writeFailure = null
        assertEquals(setOf("rain"), adapter.observe("sound").first().ids)

        store.writeFailure = CancellationException("cancelled")
        assertFailsWith<CancellationException> { adapter.setFavorite("sound", "rain", false) }
    }

    @Test
    fun identicalIdsInDifferentNamespacesAreIndependentAndPersist() = runTest {
        val store = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(store)
        adapter.toggle("sound", "shared-id")
        adapter.toggle("story", "shared-id")
        adapter.toggle("sound", "shared-id")

        val reopened = DataStoreFavoritesAdapter(store)
        assertEquals(emptySet(), reopened.observe("sound").first { it.isAvailable }.ids)
        assertEquals(setOf("shared-id"), reopened.observe("story").first { it.isAvailable }.ids)
    }

    @Test
    fun favoritesAlreadyInTheStoreSurviveOtherNamespacesAndToggles() = runTest {
        val store = FakePreferencesDataStore().apply { store(setOf("gentle-rain")) }
        val adapter = DataStoreFavoritesAdapter(store)
        adapter.toggle("story", "night")
        assertEquals(setOf("gentle-rain"), adapter.observe("sound").first { it.isAvailable }.ids)
        adapter.toggle("sound", "gentle-rain")
        assertEquals(emptySet(), DataStoreFavoritesAdapter(store).observe("sound").first { it.isAvailable }.ids)
        assertEquals(setOf("night"), adapter.observe("story").first { it.isAvailable }.ids)
    }

    @Test
    fun invalidKeysFailBeforeWriting() = runTest {
        val adapter = DataStoreFavoritesAdapter(FakePreferencesDataStore())
        assertFailsWith<IllegalArgumentException> { adapter.observe("") }
        assertFailsWith<IllegalArgumentException> { adapter.toggle("../story", "id") }
        assertFailsWith<IllegalArgumentException> { adapter.toggle("story", " ") }
        assertEquals(emptySet(), adapter.observe("story").first { it.isAvailable }.ids)
    }

    @Test
    fun togglingAddsAndThenRemovesTheId() = runTest {
        val adapter = DataStoreFavoritesAdapter(FakePreferencesDataStore())

        adapter.toggle("sound", "gentle-rain")
        assertEquals(setOf("gentle-rain"), adapter.observe("sound").first { it.isAvailable }.ids)

        adapter.toggle("sound", "calm-waves")
        assertEquals(
            setOf("gentle-rain", "calm-waves"),
            adapter.observe("sound").first { it.isAvailable }.ids,
        )

        adapter.toggle("sound", "gentle-rain")
        assertEquals(setOf("calm-waves"), adapter.observe("sound").first { it.isAvailable }.ids)
    }

    @Test
    fun aTransientReadFailureIsRetriedRatherThanLeavingFavoritesEmptyForever() = runTest {
        val dataStore = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(dataStore)
        adapter.toggle("sound", "gentle-rain")
        dataStore.failingReads = 5

        assertEquals(setOf("gentle-rain"), adapter.observe("sound").first { it.isAvailable }.ids)
        assertEquals(0, dataStore.failingReads)
    }

    @Test
    fun readFailuresAreReportedAsUnavailable() = runTest {
        val dataStore = FakePreferencesDataStore().apply { failingReads = Int.MAX_VALUE }
        val adapter = DataStoreFavoritesAdapter(dataStore)

        assertEquals(listOf(FavoritesSnapshot(emptySet(), isAvailable = false)), adapter.observe("sound").take(1).toList())
    }

    @Test
    fun aFailedWriteReturnsFailureWithoutChangingTheStoredIds() = runTest {
        val dataStore = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(dataStore)
        adapter.toggle("sound", "gentle-rain")

        dataStore.writeFailure = IllegalStateException("disk is full")
        assertEquals(FavoriteToggleResult.Unavailable, adapter.toggle("sound", "calm-waves"))

        dataStore.writeFailure = null
        assertEquals(setOf("gentle-rain"), adapter.observe("sound").first { it.isAvailable }.ids)
    }

    @Test
    fun aStoredIdThatCannotNameATrackIsDroppedRatherThanThrown() = runTest {
        val dataStore = FakePreferencesDataStore()
        dataStore.store(setOf("gentle-rain", "", "   ", "calm-waves"))
        val adapter = DataStoreFavoritesAdapter(dataStore)

        assertEquals(
            setOf("gentle-rain", "calm-waves"),
            adapter.observe("sound").first { it.isAvailable }.ids,
        )
    }

    @Test
    fun anExistingCollectorRetainsIdsAndRecoversAfterAReadFailure() = runTest {
        val store = FakePreferencesDataStore().apply { store(setOf("rain")) }
        val adapter = DataStoreFavoritesAdapter(store)
        val snapshots = mutableListOf<FavoritesSnapshot>()
        backgroundScope.launch { adapter.observe("sound").collect { snapshots += it } }
        runCurrent()
        assertEquals(FavoritesSnapshot(setOf("rain")), snapshots.last())

        store.readable.value = false
        runCurrent()
        assertEquals(FavoritesSnapshot(setOf("rain"), isAvailable = false), snapshots.last())
        assertEquals(FavoriteToggleResult.Updated, adapter.toggle("sound", "ocean"))
        store.readable.value = true
        advanceTimeBy(150)
        runCurrent()
        assertEquals(FavoritesSnapshot(setOf("rain", "ocean")), snapshots.last())
    }

    @Test
    fun cancellingAnObserverStopsRetriesAndWriteCancellationPropagates() = runTest {
        val store = FakePreferencesDataStore().apply { failingReads = 100 }
        val adapter = DataStoreFavoritesAdapter(store)
        val observer = launch { adapter.observe("sound").collect() }
        runCurrent()
        observer.cancelAndJoin()
        val remainingFailures = store.failingReads
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(remainingFailures, store.failingReads)

        store.writeFailure = CancellationException("cancelled")
        assertFailsWith<CancellationException> { adapter.toggle("sound", "rain") }
    }
    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val stored = MutableStateFlow<Preferences>(emptyPreferences())
        val readable = MutableStateFlow(true)
        var failingReads: Int = 0
        var writeFailure: Throwable? = null
        var beforeNextWrite: (suspend () -> Unit)? = null
        private val writeMutex = Mutex()

        /** Puts ids straight into the store, including values the adapter would never write. */
        fun store(ids: Set<String>) {
            stored.value = mutablePreferencesOf(stringSetPreferencesKey("favorite_sound_ids") to ids)
        }

        override val data: Flow<Preferences> = flow {
            if (failingReads > 0) {
                failingReads--
                throw IllegalStateException("the preferences file is unreadable")
            }
            emitAll(combine(stored, readable) { preferences, available ->
                check(available) { "temporarily unreadable" }
                preferences
            })
        }

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = writeMutex.withLock {
            writeFailure?.let { throw it }
            val beforeWrite = beforeNextWrite
            beforeNextWrite = null
            beforeWrite?.invoke()
            transform(stored.value).also { stored.value = it }
        }
    }
}
