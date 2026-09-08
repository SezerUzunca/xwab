package com.xwab.app.core.favorites.port

import kotlinx.coroutines.flow.Flow

/**
 * Persists favorites independently for each caller-owned namespace. Namespaces use lowercase
 * letters, digits, underscores or hyphens (1–64 characters). Item IDs must be nonblank.
 *
 * A namespace is part of the stored key, so it is a name an installed copy of the app already has
 * on disk: renaming one drops every favorite saved under it. Sounds pass `music` for that reason
 * rather than `sound` — it is the name the store was first written with.
 */
interface FavoritesPort {
    fun observe(namespace: String): Flow<Set<String>>

    suspend fun toggle(namespace: String, itemId: String)
}
