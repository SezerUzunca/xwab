package com.xwab.app.testing

import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoritesSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * An in-memory [FavoritesPort], as namespace-neutral as the port it stands in for.
 *
 * Seeded per namespace because a namespace is the caller's to choose. A content type's own testing
 * module adds the builder for its namespace — `:testing:sound` does for sounds — so this one never
 * names a content type, and a test that declares it compiles against no catalog.
 *
 * @param initial the ids each namespace starts with.
 */
class FakeFavorites(initial: Map<String, Set<String>> = emptyMap()) : FavoritesPort {
    private val state = MutableStateFlow(initial)
    val toggles = mutableListOf<Pair<String, String>>()
    val sets = mutableListOf<Triple<String, String, Boolean>>()
    val available = MutableStateFlow(true)
    var toggleResult = FavoriteToggleResult.Updated

    override fun observe(namespace: String): Flow<FavoritesSnapshot> = combine(state, available) { ids, readable ->
        FavoritesSnapshot(ids[namespace].orEmpty(), readable)
    }

    override suspend fun toggle(namespace: String, itemId: String): FavoriteToggleResult {
        if (toggleResult == FavoriteToggleResult.Unavailable) return toggleResult
        toggles += namespace to itemId
        state.update { ids ->
            val current = ids[namespace].orEmpty()
            ids + (namespace to if (itemId in current) current - itemId else current + itemId)
        }
        return FavoriteToggleResult.Updated
    }

    override suspend fun setFavorite(namespace: String, itemId: String, isFavorite: Boolean): FavoriteToggleResult {
        if (toggleResult == FavoriteToggleResult.Unavailable) return toggleResult
        sets += Triple(namespace, itemId, isFavorite)
        state.update { ids ->
            val current = ids[namespace].orEmpty()
            ids + (namespace to if (isFavorite) current + itemId else current - itemId)
        }
        return FavoriteToggleResult.Updated
    }
}
