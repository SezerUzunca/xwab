package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import co.touchlab.kermit.Logger
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoritesSnapshot
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

internal class DataStoreFavoritesAdapter(
    private val dataStore: DataStore<Preferences>,
) : FavoritesPort {
    private val logger = Logger.withTag("FavoritesPort")

    override fun observe(namespace: String): Flow<FavoritesSnapshot> {
        val favoriteIdsKey = idsKey(namespace)
        return flow {
            var lastIds = emptySet<String>()
            emitAll(dataStore.data
                .map { preferences ->
                    lastIds = preferences[favoriteIdsKey].orEmpty()
                        .filterTo(mutableSetOf()) { it.isNotBlank() }
                    FavoritesSnapshot(lastIds)
                }
                .retryWhen { error, attempt ->
                    if (error is CancellationException || error !is Exception) throw error
                    logger.w(error) { "Could not read the favorites; retrying." }
                    emit(FavoritesSnapshot(lastIds, isAvailable = false))
                    delay((RETRY_DELAY_MS shl attempt.coerceAtMost(5L).toInt()).milliseconds)
                    true
                })
        }
    }

    override suspend fun toggle(namespace: String, itemId: String): FavoriteToggleResult {
        val favoriteIdsKey = idsKey(namespace)
        require(itemId.isNotBlank()) { "Favorite IDs must not be blank." }
        return try {
            dataStore.edit { preferences ->
                val current = preferences[favoriteIdsKey].orEmpty()
                preferences[favoriteIdsKey] =
                    if (itemId in current) current - itemId else current + itemId
            }
            FavoriteToggleResult.Updated
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            logger.e(error) { "Could not persist the favorite toggle for $namespace/$itemId." }
            FavoriteToggleResult.Unavailable
        }
    }

    private fun idsKey(namespace: String): Preferences.Key<Set<String>> {
        require(namespace.matches(Regex("[a-z0-9][a-z0-9_-]{0,63}"))) { "Invalid favorites namespace." }
        return stringSetPreferencesKey("favorite_${namespace}_ids")
    }

    private companion object {
        const val RETRY_DELAY_MS = 150L
    }
}
