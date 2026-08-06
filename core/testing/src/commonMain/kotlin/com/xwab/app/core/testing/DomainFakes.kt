package com.xwab.app.core.testing

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory stand-ins for the three domain ports, plus the two builders that make a test's setup
 * one line.
 *
 * Every feature owns a use case that joins catalog, favorites and playback into one screen model,
 * so every feature needs the same three fakes. They live here rather than being copied into each
 * feature's test source set — a new feature gets them from `xwab.kmp.feature` without asking.
 *
 * This module is only ever on a test compile classpath; nothing in `commonMain` of the app
 * depends on it.
 */
fun track(id: String, categoryId: String = "rain") = Music(
    id = TrackId(id),
    name = id,
    categoryId = categoryId,
    durationSeconds = 60,
)

fun category(id: String, musicCount: Int = 0) = Category(
    id = id,
    name = id,
    description = "",
    symbol = "*",
    musicCount = musicCount,
)

class FakeMusicCatalog(
    private val categories: List<Category> = emptyList(),
    private val tracks: List<Music> = emptyList(),
) : MusicCatalogRepository {
    override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
    override fun observeAllMusic(): Flow<List<Music>> = flowOf(tracks)
    override fun observeCategory(categoryId: String): Flow<Category?> =
        flowOf(categories.find { it.id == categoryId })

    override fun observeMusicForCategory(categoryId: String): Flow<List<Music>> =
        flowOf(tracks.filter { it.categoryId == categoryId })

    override fun observeMusic(musicId: TrackId): Flow<Music?> = flowOf(tracks.find { it.id == musicId })
}

class FakeFavorites(favoriteIds: Set<TrackId> = emptySet()) : FavoritesRepository {
    private val state = MutableStateFlow(favoriteIds)
    val toggles = mutableListOf<TrackId>()

    override val favoriteIds: Flow<Set<TrackId>> = state

    override suspend fun toggle(musicId: TrackId) {
        toggles += musicId
        state.value = if (musicId in state.value) state.value - musicId else state.value + musicId
    }
}

class FakePlaybackCoordinator : PlaybackCoordinator {
    private val summary = MutableStateFlow(PlaybackSummary())
    private val remainingMs = MutableStateFlow<Long?>(null)

    override val playback: Flow<PlaybackSummary> = summary
    override val sleepTimerRemainingMs: Flow<Long?> = remainingMs

    var playedTrackId: TrackId? = null
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

    override suspend fun play(trackId: TrackId) {
        playedTrackId = trackId
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
