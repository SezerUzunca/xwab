package com.xwab.app.core.catalog

import kotlinx.coroutines.flow.Flow

/**
 * Read access to the sound catalog: the tracks and the categories that group them.
 *
 * This is the whole of what a screen may know about the catalog. The shipped manifest, the HTTPS
 * source behind each track and the name it caches under live in `core:catalog-manifest`, which no
 * feature declares — so nothing reachable from here leads to a URL or a file name.
 */
interface MusicCatalogRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeAllMusic(): Flow<List<Music>>
    fun observeCategory(categoryId: String): Flow<Category?>
    fun observeMusicForCategory(categoryId: String): Flow<List<Music>>
    fun observeMusic(musicId: TrackId): Flow<Music?>
}
