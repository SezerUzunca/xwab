package com.xwab.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CategoryViewModel(
    categoryId: String,
    observeCategoryContentUseCase: ObserveCategoryContentUseCase,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) : ViewModel() {
    val state: StateFlow<CategoryState> = observeCategoryContentUseCase(categoryId)
        .map { content ->
            CategoryState(
                category = content.category,
                musics = content.musics,
                favoriteIds = content.favoriteIds,
                requestedTrackId = content.playback.requestedTrackId,
                playIntent = content.playback.playIntent,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryState(),
        )

    fun toggleFavorite(musicId: TrackId) {
        viewModelScope.launch { favoritesRepository.toggle(musicId) }
    }

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(musicId: TrackId) {
        val current = state.value
        if (current.requestedTrackId == musicId && current.playIntent) {
            playbackCoordinator.pause()
        } else {
            viewModelScope.launch { playbackCoordinator.play(musicId) }
        }
    }
}
