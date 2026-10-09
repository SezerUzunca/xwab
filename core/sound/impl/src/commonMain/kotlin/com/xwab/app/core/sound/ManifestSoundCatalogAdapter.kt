package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves metadata from one immutable manifest. */
@SingleIn(SoundScope::class)
@Inject
internal class ManifestSoundCatalogAdapter internal constructor(
    tracks: List<Track>,
    categories: List<Category>,
    private val delivery: DeliveryPort,
    private val sources: SoundSources,
) : SoundPort {
    init {
        val duplicates = tracks.groupBy(Track::id).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Track ids must be unique: ${duplicates.joinToString { it.value }}"
        }
    }

    private val allTracks = flowOf(tracks)
    private val trackIds = tracks.mapTo(mutableSetOf(), Track::id)
    private val allCategories = flowOf(categories)

    override fun observeCategories(): Flow<List<Category>> = allCategories
    override fun observeAllTracks(): Flow<List<Track>> = allTracks
    override fun observeCategory(categoryId: CategoryId): Flow<Category?> =
        allCategories.map { values -> values.find { it.id == categoryId } }
    override fun observeTracksForCategory(categoryId: CategoryId): Flow<List<Track>> =
        allTracks.map { values -> values.filter { it.categoryId == categoryId } }
    override fun observeTrack(trackId: TrackId): Flow<Track?> =
        allTracks.map { values -> values.find { it.id == trackId } }

    override fun observeOfflineReady(trackId: TrackId): Flow<Boolean> {
        val request = sources.requestFor(trackId).takeIf { trackId in trackIds }
        return request?.let { delivery.observeCached(it.key) } ?: flowOf(false)
    }
}
