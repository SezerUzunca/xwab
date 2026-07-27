package com.xwab.app.core.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoritesRepository {
    val favoriteIds: Flow<Set<String>>
    suspend fun toggle(musicId: String)
}

internal class DataStoreFavoritesRepository(private val dataStore: DataStore<Preferences>) : FavoritesRepository {
    private val favoriteIdsKey = stringSetPreferencesKey("favorite_music_ids")
    override val favoriteIds: Flow<Set<String>> = dataStore.data.map { it[favoriteIdsKey].orEmpty() }

    override suspend fun toggle(musicId: String) {
        dataStore.edit { preferences ->
            val current = preferences[favoriteIdsKey].orEmpty()
            preferences[favoriteIdsKey] = if (musicId in current) current - musicId else current + musicId
        }
    }
}
