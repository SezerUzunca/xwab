package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.xwab.app.core.catalog.TrackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class DataStoreFavoritesRepositoryTest {
    @Test
    fun togglingAddsAndThenRemovesTheId() = runBlocking {
        val repository = DataStoreFavoritesRepository(FakePreferencesDataStore())

        repository.toggle(TrackId("gentle-rain"))
        assertEquals(setOf(TrackId("gentle-rain")), repository.favoriteIds.first())

        repository.toggle(TrackId("calm-waves"))
        assertEquals(setOf(TrackId("gentle-rain"), TrackId("calm-waves")), repository.favoriteIds.first())

        repository.toggle(TrackId("gentle-rain"))
        assertEquals(setOf(TrackId("calm-waves")), repository.favoriteIds.first())
    }

    @Test
    fun aTransientReadFailureIsRetriedRatherThanLeavingFavoritesEmptyForever() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        val repository = DataStoreFavoritesRepository(dataStore)
        repository.toggle(TrackId("gentle-rain"))
        dataStore.failingReads = 2

        assertEquals(setOf(TrackId("gentle-rain")), repository.favoriteIds.first())
        assertEquals(0, dataStore.failingReads)
    }

    @Test
    fun readsThatKeepFailingDegradeToEmptyInsteadOfTerminatingTheScreenFlows() = runBlocking {
        val dataStore = FakePreferencesDataStore().apply { failingReads = Int.MAX_VALUE }
        val repository = DataStoreFavoritesRepository(dataStore)

        assertEquals(listOf(emptySet<TrackId>()), repository.favoriteIds.take(1).toList())
    }

    @Test
    fun aFailedWriteDoesNotCancelTheCallingScope() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        val repository = DataStoreFavoritesRepository(dataStore)
        repository.toggle(TrackId("gentle-rain"))

        dataStore.writeFailure = IllegalStateException("disk is full")
        repository.toggle(TrackId("calm-waves"))

        dataStore.writeFailure = null
        assertEquals(setOf(TrackId("gentle-rain")), repository.favoriteIds.first())
    }

    /**
     * The store outlives the code that wrote it, so what comes out of it is not assumed to be
     * well-formed. A blank id cannot name a track — `TrackId` refuses one — and turning it into an
     * exception here would take the whole set, and the screen collecting it, down with it.
     */
    @Test
    fun aStoredIdThatCannotNameATrackIsDroppedRatherThanThrown() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        dataStore.store(setOf("gentle-rain", "", "   ", "calm-waves"))
        val repository = DataStoreFavoritesRepository(dataStore)

        assertEquals(
            setOf(TrackId("gentle-rain"), TrackId("calm-waves")),
            repository.favoriteIds.first(),
        )
    }

    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val stored = MutableStateFlow<Preferences>(emptyPreferences())
        var failingReads: Int = 0
        var writeFailure: Throwable? = null

        /** Puts ids straight into the store, including ones the repository would never write. */
        fun store(ids: Set<String>) {
            stored.value = mutablePreferencesOf(stringSetPreferencesKey("favorite_music_ids") to ids)
        }

        override val data: Flow<Preferences> = flow {
            if (failingReads > 0) {
                failingReads--
                throw IllegalStateException("the preferences file is unreadable")
            }
            emitAll(stored)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            writeFailure?.let { throw it }
            return transform(stored.value).also { stored.value = it }
        }
    }
}
