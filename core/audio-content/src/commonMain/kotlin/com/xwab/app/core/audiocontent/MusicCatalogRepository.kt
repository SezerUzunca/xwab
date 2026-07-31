package com.xwab.app.core.audiocontent

import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Read access to the sound catalog: the tracks and the categories that group them. */
interface MusicCatalogRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeAllMusic(): Flow<List<Music>>
    fun observeCategory(categoryId: String): Flow<Category?>
    fun observeMusicForCategory(categoryId: String): Flow<List<Music>>
    fun observeMusic(musicId: String): Flow<Music?>
}

/**
 * Serves the shipped catalog to the screens.
 *
 * This is the half of the module a screen is allowed to see: plain `Music` and `Category` values
 * with the manifest's physical sources left behind. It only queries lists, so it has no lifecycle —
 * nothing to start, close or cancel. The constructor takes the two lists so a test can query a
 * small fixture instead of the real catalog.
 */
internal class DefaultMusicCatalogRepository(
    tracks: List<Music> = catalogEntries.map(CatalogEntry::music),
    categories: List<Category> = catalogCategories,
) : MusicCatalogRepository {
    private val allTracks: Flow<List<Music>> = flowOf(tracks)
    private val allCategories: Flow<List<Category>> = flowOf(categories)

    override fun observeCategories(): Flow<List<Category>> = allCategories

    override fun observeAllMusic(): Flow<List<Music>> = allTracks

    override fun observeCategory(categoryId: String): Flow<Category?> =
        allCategories.map { values -> values.find { it.id == categoryId } }

    override fun observeMusicForCategory(categoryId: String): Flow<List<Music>> =
        allTracks.map { values -> values.filter { it.categoryId == categoryId } }

    override fun observeMusic(musicId: String): Flow<Music?> =
        allTracks.map { values -> values.find { it.id == musicId } }
}
