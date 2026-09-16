package com.xwab.app.core.favorites.port

import kotlinx.coroutines.flow.Flow

/**
 * Persists favorites independently for each caller-owned namespace. Namespaces use lowercase
 * letters, digits, underscores or hyphens (1–64 characters). Item IDs must be nonblank.
 *
 * A namespace is part of the stored key, so once an app has shipped it is a name installed copies
 * already hold on disk: renaming one then drops every favorite saved under it.
 */
interface FavoritesPort {
    /** Reports read failures with the last known IDs and retries while collected. */
    fun observe(namespace: String): Flow<FavoritesSnapshot>

    /** Storage failures are returned to the caller; cancellation still propagates. */
    suspend fun toggle(namespace: String, itemId: String): FavoriteToggleResult
}

/** IDs are the last successful read, or empty before the first successful read. */
data class FavoritesSnapshot(val ids: Set<String>, val isAvailable: Boolean = true)

enum class FavoriteToggleResult {
    Updated,
    Unavailable,
}
