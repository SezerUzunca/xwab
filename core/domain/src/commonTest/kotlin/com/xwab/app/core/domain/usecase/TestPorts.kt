package com.xwab.app.core.domain.usecase

import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import com.xwab.app.core.domain.port.PlaybackSummary
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal fun track(id: String, categoryId: String = "rain") = Music(
    id = id,
    name = id,
    categoryId = categoryId,
    durationSeconds = 60,
)

internal fun category(id: String, musicCount: Int = 0) = Category(
    id = id,
    name = id,
    description = "",
    symbol = "*",
    musicCount = musicCount,
)

internal class FakeMusicCatalog(
    private val categories: List<Category> = emptyList(),
    private val tracks: List<Music> = emptyList(),
) : MusicCatalogRepository {
    override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
    override fun observeAllMusic(): Flow<List<Music>> = flowOf(tracks)
    override fun observeCategory(categoryId: String): Flow<Category?> =
        flowOf(categories.find { it.id == categoryId })

    override fun observeMusicForCategory(categoryId: String): Flow<List<Music>> =
        flowOf(tracks.filter { it.categoryId == categoryId })

    override fun observeMusic(musicId: String): Flow<Music?> = flowOf(tracks.find { it.id == musicId })
}

internal class FakeFavorites(favoriteIds: Set<String> = emptySet()) : FavoritesRepository {
    private val state = MutableStateFlow(favoriteIds)
    val toggles = mutableListOf<String>()

    override val favoriteIds: Flow<Set<String>> = state

    override suspend fun toggle(musicId: String) {
        toggles += musicId
        state.value = if (musicId in state.value) state.value - musicId else state.value + musicId
    }
}

internal class FakePlaybackCoordinator : PlaybackCoordinator {
    private val summary = MutableStateFlow(PlaybackSummary())
    private val remainingMs = MutableStateFlow<Long?>(null)

    override val playback: Flow<PlaybackSummary> = summary
    override val sleepTimerRemainingMs: Flow<Long?> = remainingMs

    var toggledTrack: Music? = null
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

    override suspend fun togglePlayback(music: Music) {
        toggledTrack = music
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
