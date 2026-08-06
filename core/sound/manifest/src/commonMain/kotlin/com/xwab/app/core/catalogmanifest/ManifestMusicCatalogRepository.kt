package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Serves the shipped manifest to the screens as plain `Music` and `Category` values, with the
 * physical sources left behind.
 *
 * It only queries lists, so it has no lifecycle — nothing to start, close or cancel. The
 * constructor takes the two lists so a test can query a small fixture instead of the real catalog.
 */
internal class ManifestMusicCatalogRepository(
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

    override fun observeMusic(musicId: TrackId): Flow<Music?> =
        allTracks.map { values -> values.find { it.id == musicId } }
}
