package com.xwab.app.core.domain.port

import kotlinx.coroutines.flow.Flow

/** The tracks the user marked as favorites, kept across launches. */
interface FavoritesRepository {
    val favoriteIds: Flow<Set<String>>
    suspend fun toggle(musicId: String)
}
