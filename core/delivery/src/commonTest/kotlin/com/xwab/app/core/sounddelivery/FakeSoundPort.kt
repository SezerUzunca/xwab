package com.xwab.app.core.sounddelivery

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackSource

/**
 * A manifest holding exactly the tracks one test cares about.
 *
 * Delivery reads the catalog through a port now, so its tests no longer reach into the shipped
 * manifest and pick a name out of it — which used to tie a cache assertion to whichever track
 * happened to be listed first.
 */
internal class FakeSoundPort(
    private val sources: Map<TrackId, TrackSource> = emptyMap(),
) : SoundPort {
    override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
    override fun observeAllMusic(): Flow<List<Music>> = flowOf(emptyList())
    override fun observeCategory(categoryId: CategoryId): Flow<Category?> = flowOf(null)
    override fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>> = flowOf(emptyList())
    override fun observeMusic(trackId: TrackId): Flow<Music?> = flowOf(null)
    override val cacheFileNames: Set<String> =
        sources.values.mapTo(mutableSetOf()) { it.cacheFileName }

    override fun sourceFor(trackId: TrackId): TrackSource? = sources[trackId]
}

/**
 * A catalog that refers to [cacheFileNames] and nothing else, which is all the sweep ever asks of
 * it. The track ids are derived from the names so the fake stays a one-liner at the call site.
 */
internal fun sourcePortKeeping(vararg cacheFileNames: String) = FakeSoundPort(
    cacheFileNames.associate { name ->
        TrackId(name.substringBeforeLast("-v")) to TrackSource(name, "https://example.test/$name")
    },
)
