package com.xwab.app.core.sound

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.TrackSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves metadata and sources from one immutable manifest. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class SoundPortImpl internal constructor(
    entries: List<CatalogEntry>,
    categories: List<Category> = emptyList(),
) : SoundPort {
    @Inject
    internal constructor() : this(catalogEntries, catalogCategories)

    init {
        val duplicates = entries.groupBy { it.music.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Track ids must be unique: ${duplicates.joinToString { it.value }}"
        }
    }

    private val allTracks = flowOf(entries.map(CatalogEntry::music))
    private val allCategories = flowOf(categories)
    private val sourcesById = entries.associate { entry ->
        entry.music.id to TrackSource(entry.cacheFileName, entry.httpsUrl)
    }

    override val cacheFileNames: Set<String> = entries.mapTo(mutableSetOf()) { it.cacheFileName }
    override fun sourceFor(trackId: TrackId): TrackSource? = sourcesById[trackId]
    override fun observeCategories(): Flow<List<Category>> = allCategories
    override fun observeAllMusic(): Flow<List<Music>> = allTracks
    override fun observeCategory(categoryId: CategoryId): Flow<Category?> =
        allCategories.map { values -> values.find { it.id == categoryId } }
    override fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>> =
        allTracks.map { values -> values.filter { it.categoryId == categoryId } }
    override fun observeMusic(trackId: TrackId): Flow<Music?> =
        allTracks.map { values -> values.find { it.id == trackId } }
}
