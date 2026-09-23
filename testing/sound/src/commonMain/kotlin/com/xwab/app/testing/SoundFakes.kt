package com.xwab.app.testing

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The sound catalog's stand-in, plus the builders that make a sound test's setup one line.
 *
 * Kept apart from the session and favorites fakes on purpose. They used to share one module, and
 * that module's dependency on `:core:sound` travelled onto the test classpath of every feature that
 * declared it — the story list and the now-playing bar included, neither of which has anything to
 * do with sounds. Removing the sound capability then broke tests that never read one.
 *
 * This module is only ever on a test compile classpath; nothing in `commonMain` of the app
 * depends on it.
 */
fun track(id: String, categoryId: String = "rain") = Track(
    id = TrackId(id),
    name = id,
    categoryId = CategoryId(categoryId),
    durationSeconds = 60,
)

fun category(id: String, trackCount: Int = 0) = Category(
    id = CategoryId(id),
    name = id,
    description = "",
    symbol = "*",
    trackCount = trackCount,
)

class FakeSoundCatalog(
    private val categories: List<Category> = emptyList(),
    private val tracks: List<Track> = emptyList(),
) : SoundPort {
    override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
    override fun observeAllTracks(): Flow<List<Track>> = flowOf(tracks)
    override fun observeCategory(categoryId: CategoryId): Flow<Category?> =
        flowOf(categories.find { it.id == categoryId })

    override fun observeTracksForCategory(categoryId: CategoryId): Flow<List<Track>> =
        flowOf(tracks.filter { it.categoryId == categoryId })

    override fun observeTrack(trackId: TrackId): Flow<Track?> = flowOf(tracks.find { it.id == trackId })
}

/**
 * A [FakeFavorites] already holding [favoriteIds] under the namespace sounds are saved in.
 *
 * Named like the class it builds, so a sound test still reads `FakeFavorites(setOf(rain.id))`.
 * Which namespace that is belongs to the sound capability, so it is stated here rather than in the
 * favorites fake, which — like the port it stands in for — knows no content type.
 */
fun FakeFavorites(favoriteIds: Set<TrackId>): FakeFavorites =
    FakeFavorites(mapOf(SOUND_FAVORITES_NAMESPACE to favoriteIds.mapTo(mutableSetOf()) { it.value }))
