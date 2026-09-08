package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.kermit.Logger
import com.xwab.app.core.favorites.port.FavoritesPort
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

internal class DataStoreFavoritesAdapter(
    private val dataStore: DataStore<Preferences>,
) : FavoritesPort {
    private val logger = Logger.withTag("FavoritesPort")

    override fun observe(namespace: String): Flow<Set<String>> {
        val favoriteIdsKey = idsKey(namespace)
        return dataStore.data
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
            .map { preferences ->
                preferences[favoriteIdsKey].orEmpty()
                    .filterTo(mutableSetOf()) { it.isNotBlank() }
            }
    }

    override suspend fun toggle(namespace: String, itemId: String) {
        val favoriteIdsKey = idsKey(namespace)
        require(itemId.isNotBlank()) { "Favorite IDs must not be blank." }
        try {
            dataStore.edit { preferences ->
                val current = preferences[favoriteIdsKey].orEmpty()
                preferences[favoriteIdsKey] =
                    if (itemId in current) current - itemId else current + itemId
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logger.e(error) { "Could not persist the favorite toggle for $namespace/$itemId." }
        }
    }

    private fun idsKey(namespace: String): Preferences.Key<Set<String>> {
        require(namespace.matches(Regex("[a-z0-9][a-z0-9_-]{0,63}"))) { "Invalid favorites namespace." }
        return stringSetPreferencesKey("favorite_${namespace}_ids")
    }

    private companion object {
        const val MAX_READ_RETRIES = 3L
        const val RETRY_DELAY_MS = 150L
    }
}
