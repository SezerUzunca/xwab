package com.xwab.app.core.data

import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow

/** Read access to the sound catalog: the tracks and the categories that group them. */
interface MusicCatalogRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeAllMusic(): Flow<List<Music>>
    fun observeCategory(categoryId: String): Flow<Category?>
    fun observeMusicForCategory(categoryId: String): Flow<List<Music>>
    fun observeMusic(musicId: String): Flow<Music?>
}
