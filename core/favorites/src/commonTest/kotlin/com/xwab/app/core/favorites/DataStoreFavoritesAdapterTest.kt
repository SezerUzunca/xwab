package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class DataStoreFavoritesAdapterTest {
    @Test
    fun identicalIdsInDifferentNamespacesAreIndependentAndPersist() = runBlocking {
        val store = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(store)
        adapter.toggle("music", "shared-id")
        adapter.toggle("story", "shared-id")
        adapter.toggle("music", "shared-id")

        val reopened = DataStoreFavoritesAdapter(store)
        assertEquals(emptySet(), reopened.observe("music").first())
        assertEquals(setOf("shared-id"), reopened.observe("story").first())
    }

    @Test
    fun legacyMusicFavoritesSurviveOtherNamespacesAndToggles() = runBlocking {
        val store = FakePreferencesDataStore().apply { store(setOf("gentle-rain")) }
        val adapter = DataStoreFavoritesAdapter(store)
        adapter.toggle("story", "night")
        assertEquals(setOf("gentle-rain"), adapter.observe("music").first())
        adapter.toggle("music", "gentle-rain")
        assertEquals(emptySet(), DataStoreFavoritesAdapter(store).observe("music").first())
        assertEquals(setOf("night"), adapter.observe("story").first())
    }

    @Test
    fun invalidKeysFailBeforeWriting() = runBlocking {
        val adapter = DataStoreFavoritesAdapter(FakePreferencesDataStore())
        assertFailsWith<IllegalArgumentException> { adapter.observe("") }
        assertFailsWith<IllegalArgumentException> { adapter.toggle("../story", "id") }
        assertFailsWith<IllegalArgumentException> { adapter.toggle("story", " ") }
        assertEquals(emptySet(), adapter.observe("story").first())
    }

    @Test
    fun togglingAddsAndThenRemovesTheId() = runBlocking {
        val adapter = DataStoreFavoritesAdapter(FakePreferencesDataStore())

        adapter.toggle("music", "gentle-rain")
        assertEquals(setOf("gentle-rain"), adapter.observe("music").first())

        adapter.toggle("music", "calm-waves")
        assertEquals(
            setOf("gentle-rain", "calm-waves"),
            adapter.observe("music").first(),
        )

        adapter.toggle("music", "gentle-rain")
        assertEquals(setOf("calm-waves"), adapter.observe("music").first())
    }

    @Test
    fun aTransientReadFailureIsRetriedRatherThanLeavingFavoritesEmptyForever() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(dataStore)
        adapter.toggle("music", "gentle-rain")
        dataStore.failingReads = 2

        assertEquals(setOf("gentle-rain"), adapter.observe("music").first())
        assertEquals(0, dataStore.failingReads)
    }

    @Test
    fun readsThatKeepFailingDegradeToEmptyInsteadOfTerminatingTheScreenFlows() = runBlocking {
        val dataStore = FakePreferencesDataStore().apply { failingReads = Int.MAX_VALUE }
        val adapter = DataStoreFavoritesAdapter(dataStore)

        assertEquals(listOf(emptySet<String>()), adapter.observe("music").take(1).toList())
    }

    @Test
    fun aFailedWriteDoesNotCancelTheCallingScope() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        val adapter = DataStoreFavoritesAdapter(dataStore)
        adapter.toggle("music", "gentle-rain")

        dataStore.writeFailure = IllegalStateException("disk is full")
        adapter.toggle("music", "calm-waves")

        dataStore.writeFailure = null
        assertEquals(setOf("gentle-rain"), adapter.observe("music").first())
    }

    @Test
    fun aStoredIdThatCannotNameATrackIsDroppedRatherThanThrown() = runBlocking {
        val dataStore = FakePreferencesDataStore()
        dataStore.store(setOf("gentle-rain", "", "   ", "calm-waves"))
        val adapter = DataStoreFavoritesAdapter(dataStore)

        assertEquals(
            setOf("gentle-rain", "calm-waves"),
            adapter.observe("music").first(),
        )
    }

    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val stored = MutableStateFlow<Preferences>(emptyPreferences())
        var failingReads: Int = 0
        var writeFailure: Throwable? = null

        /** Puts ids straight into the store, including values the adapter would never write. */
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

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            writeFailure?.let { throw it }
            return transform(stored.value).also { stored.value = it }
        }
    }
}
