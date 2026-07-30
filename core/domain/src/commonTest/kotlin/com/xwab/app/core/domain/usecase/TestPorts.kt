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

/**
 * The same fakes `core:testing` publishes for the feature modules, kept local on purpose:
 * `core:testing` implements these ports, so depending on it from here would point `core:domain`
 * back at a module that depends on `core:domain`.
 */
internal fun track(id: String, categoryId: String = "rain") = Music(
    id = id,
    name = id,
    categoryId = categoryId,
    durationSeconds = 60,
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
    override val playback: Flow<PlaybackSummary> = MutableStateFlow(PlaybackSummary())
    override val sleepTimerRemainingMs: Flow<Long?> = MutableStateFlow<Long?>(null)

    var toggledTrack: Music? = null

    override suspend fun togglePlayback(music: Music) {
        toggledTrack = music
    }

    override fun setLooping(enabled: Boolean) = Unit
    override fun setVolume(volume: Float) = Unit
    override fun startSleepTimer(durationMs: Long) = Unit
    override fun cancelSleepTimer() = Unit
}
