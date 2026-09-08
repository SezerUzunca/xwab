package com.xwab.app.testing

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory stand-ins for the three domain ports, plus the two builders that make a test's setup
 * one line.
 *
 * Features that combine catalog, favorites and playback reuse these fakes instead of copying them
 * into each feature's test source set. Simpler features can use only the port fake they need.
 *
 * This module is only ever on a test compile classpath; nothing in `commonMain` of the app
 * depends on it.
 */
fun track(id: String, categoryId: String = "rain") = Music(
    id = TrackId(id),
    name = id,
    categoryId = CategoryId(categoryId),
    durationSeconds = 60,
)

fun category(id: String, musicCount: Int = 0) = Category(
    id = CategoryId(id),
    name = id,
    description = "",
    symbol = "*",
    musicCount = musicCount,
)

class FakeMusicCatalog(
    private val categories: List<Category> = emptyList(),
    private val tracks: List<Music> = emptyList(),
) : SoundCatalogPort {
    override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
    override fun observeAllMusic(): Flow<List<Music>> = flowOf(tracks)
    override fun observeCategory(categoryId: CategoryId): Flow<Category?> =
        flowOf(categories.find { it.id == categoryId })

    override fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>> =
        flowOf(tracks.filter { it.categoryId == categoryId })

    override fun observeMusic(trackId: TrackId): Flow<Music?> = flowOf(tracks.find { it.id == trackId })
}

class FakeFavorites(favoriteIds: Set<TrackId> = emptySet()) : FavoritesPort {
    private val state = MutableStateFlow(favoriteIds)
    val toggles = mutableListOf<TrackId>()

    override val favoriteIds: Flow<Set<TrackId>> = state

    override suspend fun toggle(trackId: TrackId) {
        toggles += trackId
        state.value = if (trackId in state.value) state.value - trackId else state.value + trackId
    }
}

class FakePlaybackPort : PlaybackPort {
    private val summary = MutableStateFlow(PlaybackSummary())
    private val remainingMs = MutableStateFlow<Long?>(null)

    override val playback: Flow<PlaybackSummary> = summary
    override val sleepTimerRemainingMs: Flow<Long?> = remainingMs

    var playedItemId: PlaybackItemId? = null
    var pauses = 0
    var looping: Boolean? = null
    var volume: Float? = null
    var startedTimerMs: Long? = null
    var cancelledTimers = 0

    fun publish(playback: PlaybackSummary) {
        summary.value = playback
    }

    fun publishSleepTimer(remaining: Long?) {
        remainingMs.value = remaining
    }

    override suspend fun play(itemId: PlaybackItemId) {
        playedItemId = itemId
    }

    override fun pause() {
        pauses++
    }

    override fun setLooping(enabled: Boolean) {
        looping = enabled
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
    }

    override fun startSleepTimer(durationMs: Long) {
        startedTimerMs = durationMs
    }

    override fun cancelSleepTimer() {
        cancelledTimers++
    }
}
