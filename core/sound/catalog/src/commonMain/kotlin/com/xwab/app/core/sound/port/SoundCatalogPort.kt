package com.xwab.app.core.sound.port

import kotlinx.coroutines.flow.Flow

/** Read access to the tracks and categories a listener can choose. */
public interface SoundCatalogPort {
    public fun observeCategories(): Flow<List<Category>>

    public fun observeAllMusic(): Flow<List<Music>>

    public fun observeCategory(categoryId: CategoryId): Flow<Category?>

    public fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>>

    public fun observeMusic(trackId: TrackId): Flow<Music?>
}
