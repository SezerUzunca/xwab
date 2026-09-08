package com.xwab.app.core.sound.port

import kotlinx.coroutines.flow.Flow

/** Read access to sound metadata and playable sources from the same catalog. */
interface SoundPort {
    fun observeCategories(): Flow<List<Category>>
    fun observeAllMusic(): Flow<List<Music>>
    fun observeCategory(categoryId: CategoryId): Flow<Category?>
    fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>>
    fun observeMusic(trackId: TrackId): Flow<Music?>

    /** The source for [trackId], or null when the catalog has no such sound. */
    fun sourceFor(trackId: TrackId): TrackSource?

    /** Every cache file name still referenced by the catalog. */
    val cacheFileNames: Set<String>
}
