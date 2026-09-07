package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.sound.port.TrackId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class ManifestSoundCatalogAdapter internal constructor(
    tracks: List<Music>,
    categories: List<Category>,
) : SoundCatalogPort {
    @Inject
    internal constructor() : this(
        tracks = catalogEntries.map(CatalogEntry::music),
        categories = catalogCategories,
    )
    private val allTracks: Flow<List<Music>> = flowOf(tracks)
    private val allCategories: Flow<List<Category>> = flowOf(categories)

    init {
        // Two tracks under one id make `observeMusic` answer with whichever came first, which is a
        // bug that looks like a content mistake. The same check `ManifestStoryCatalogAdapter`
        // makes: a copied row here can produce it, and so can a feed later.
        val duplicates = tracks.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Track ids must be unique: ${duplicates.joinToString { it.value }}"
        }
    }

    override fun observeCategories(): Flow<List<Category>> = allCategories

    override fun observeAllMusic(): Flow<List<Music>> = allTracks

    override fun observeCategory(categoryId: CategoryId): Flow<Category?> =
        allCategories.map { values -> values.find { it.id == categoryId } }

    override fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>> =
        allTracks.map { values -> values.filter { it.categoryId == categoryId } }

    override fun observeMusic(trackId: TrackId): Flow<Music?> =
        allTracks.map { values -> values.find { it.id == trackId } }
}
