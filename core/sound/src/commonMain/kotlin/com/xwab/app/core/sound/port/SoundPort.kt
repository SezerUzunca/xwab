package com.xwab.app.core.sound.port

import kotlinx.coroutines.flow.Flow

/** Read access to sound metadata. Physical sources are deliberately a separate capability. */
interface SoundPort {
    fun observeCategories(): Flow<List<Category>>
    fun observeAllMusic(): Flow<List<Music>>
    fun observeCategory(categoryId: CategoryId): Flow<Category?>
    fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>>
    fun observeMusic(trackId: TrackId): Flow<Music?>
}
