package com.xwab.app.core.sound.port

import kotlinx.coroutines.flow.Flow

/** Read access to sound metadata and playable sources from the same catalog. */
public interface SoundPort {
    public fun observeCategories(): Flow<List<Category>>
    public fun observeAllMusic(): Flow<List<Music>>
    public fun observeCategory(categoryId: CategoryId): Flow<Category?>
    public fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>>
    public fun observeMusic(trackId: TrackId): Flow<Music?>

    /** The source for [trackId], or null when the catalog has no such sound. */
    public fun sourceFor(trackId: TrackId): TrackSource?

    /** Every cache file name still referenced by the catalog. */
    public val cacheFileNames: Set<String>
}
