package com.xwab.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration.Companion.milliseconds

/** The tracks the user marked as favorites, kept across launches. */
interface FavoritesRepository {
    val favoriteIds: Flow<Set<String>>
    suspend fun toggle(musicId: String)
}

internal class DataStoreFavoritesRepository(
    private val dataStore: DataStore<Preferences>,
) : FavoritesRepository {
    private val logger = Logger.withTag("FavoritesRepository")
    private val favoriteIdsKey = stringSetPreferencesKey("favorite_music_ids")

    override val favoriteIds: Flow<Set<String>> = dataStore.data
        .retryWhen { error, attempt ->
            val willRetry = attempt < MAX_READ_RETRIES
            if (willRetry) {
                logger.w(error) { "Could not read the favorites (attempt ${attempt + 1}); retrying." }
                delay(RETRY_DELAY_MS.milliseconds)
            }
            willRetry
        }
        .catch { error ->
            logger.e(error) { "Could not read the favorites; falling back to an empty set." }
            emit(emptyPreferences())
        }
        .map { it[favoriteIdsKey].orEmpty() }

    override suspend fun toggle(musicId: String) {
        try {
            dataStore.edit { preferences ->
                val current = preferences[favoriteIdsKey].orEmpty()
                preferences[favoriteIdsKey] =
                    if (musicId in current) current - musicId else current + musicId
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logger.e(error) { "Could not persist the favorite toggle for $musicId." }
        }
    }

    private companion object {
        const val MAX_READ_RETRIES = 3L
        const val RETRY_DELAY_MS = 150L
    }
}
