package com.xwab.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.kermit.Logger
import com.xwab.app.core.domain.port.FavoritesRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

internal class DataStoreFavoritesRepository(private val dataStore: DataStore<Preferences>) : FavoritesRepository {
    private val logger = Logger.withTag("FavoritesRepository")
    private val favoriteIdsKey = stringSetPreferencesKey("favorite_music_ids")

    /**
     * Two layers, because a failed read has two very different causes.
     *
     * A transient one — the file briefly locked, or read mid-write — deserves another attempt.
     * Without one this stream would stay dead for the rest of the subscription: `catch` ends the
     * flow after its fallback, so the favorites would never come back even once the store had
     * recovered.
     *
     * A persistent one must not take the screens down with it. Favorites decorate a catalog that
     * works without them, and this stream is combined into every screen flow, so the last resort
     * is "nothing is favorited" rather than a terminated flow.
     */
    override val favoriteIds: Flow<Set<String>> = dataStore.data
        .retryWhen { error, attempt ->
            val willRetry = attempt < MAX_READ_RETRIES
            if (willRetry) {
                logger.w(error) { "Could not read the favorites (attempt ${attempt + 1}); retrying." }
                delay(RETRY_DELAY_MS)
            }
            willRetry
        }
        .catch { error ->
            logger.e(error) { "Could not read the favorites; falling back to an empty set." }
            emit(emptyPreferences())
        }
        .map { it[favoriteIdsKey].orEmpty() }

    /**
     * Callers are `viewModelScope` coroutines with no handler, so a failed write must not escape:
     * a preferences file that cannot be written is worth a log, not a crashed app. The trade-off
     * is that the caller cannot tell a dropped favorite from a stored one — surfacing that needs
     * a result type and a UI decision to go with it.
     */
    override suspend fun toggle(musicId: String) {
        try {
            dataStore.edit { preferences ->
                val current = preferences[favoriteIdsKey].orEmpty()
                preferences[favoriteIdsKey] = if (musicId in current) current - musicId else current + musicId
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
