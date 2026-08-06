package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.kermit.Logger
import com.xwab.app.core.catalog.TrackId
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

/** The tracks the user marked as favorites, kept across launches. */
interface FavoritesRepository {
    val favoriteIds: Flow<Set<TrackId>>
    suspend fun toggle(musicId: TrackId)
}

internal class DataStoreFavoritesRepository(
    private val dataStore: DataStore<Preferences>,
) : FavoritesRepository {
    private val logger = Logger.withTag("FavoritesRepository")
    private val favoriteIdsKey = stringSetPreferencesKey("favorite_music_ids")

    override val favoriteIds: Flow<Set<TrackId>> = dataStore.data
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
        // Stored as plain strings, because that is what a preferences file holds; the wrapper goes
        // back on at the boundary so nothing downstream handles a bare id again.
        .map { preferences -> preferences[favoriteIdsKey].orEmpty().mapTo(mutableSetOf(), ::TrackId) }

    override suspend fun toggle(musicId: TrackId) {
        val storedId = musicId.value
        try {
            dataStore.edit { preferences ->
                val current = preferences[favoriteIdsKey].orEmpty()
                preferences[favoriteIdsKey] =
                    if (storedId in current) current - storedId else current + storedId
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
