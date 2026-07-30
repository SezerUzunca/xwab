package com.xwab.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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

        repository.toggle("gentle-rain")
        assertEquals(setOf("gentle-rain"), repository.favoriteIds.first())

        repository.toggle("calm-waves")
        assertEquals(setOf("gentle-rain", "calm-waves"), repository.favoriteIds.first())

        repository.toggle("gentle-rain")
        assertEquals(setOf("calm-waves"), repository.favoriteIds.first())
    }

    @Test
    fun aTransientReadFailureIsRetriedRatherThanLeavingFavoritesEmptyForever() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        val repository = DataStoreFavoritesRepository(dataStore)
        repository.toggle("gentle-rain")
        dataStore.failingReads = 2

        assertEquals(setOf("gentle-rain"), repository.favoriteIds.first())
        assertEquals(0, dataStore.failingReads)
    }

    @Test
    fun readsThatKeepFailingDegradeToEmptyInsteadOfTerminatingTheScreenFlows() = runBlocking {
        val dataStore = FakePreferencesDataStore().apply { failingReads = Int.MAX_VALUE }
        val repository = DataStoreFavoritesRepository(dataStore)

        assertEquals(listOf(emptySet<String>()), repository.favoriteIds.take(1).toList())
    }

    @Test
    fun aFailedWriteDoesNotCancelTheCallingScope() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        val repository = DataStoreFavoritesRepository(dataStore)
        repository.toggle("gentle-rain")

        dataStore.writeFailure = IllegalStateException("disk is full")
        repository.toggle("calm-waves")

        dataStore.writeFailure = null
        assertEquals(setOf("gentle-rain"), repository.favoriteIds.first())
    }

    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val stored = MutableStateFlow(emptyPreferences())
        var failingReads: Int = 0
        var writeFailure: Throwable? = null

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
