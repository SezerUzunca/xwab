package com.xwab.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.domain.usecase.ToggleFavoriteUseCase
import com.xwab.app.core.domain.usecase.ToggleMusicPlaybackUseCase
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CategoryViewModel(
    categoryId: String,
    observeCategoryContentUseCase: ObserveCategoryContentUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleMusicPlaybackUseCase: ToggleMusicPlaybackUseCase,
) : ViewModel() {
    val state: StateFlow<CategoryState> = observeCategoryContentUseCase(categoryId)
        .map { content ->
            CategoryState(
                category = content.category,
                musics = content.musics,
                favoriteIds = content.favoriteIds,
                playingMusicId = content.playback.activeSourceId,
                isPlaying = content.playback.isPlaying,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryState(),
        )

    fun toggleFavorite(musicId: String) {
        viewModelScope.launch { toggleFavoriteUseCase(musicId) }
    }

    fun togglePlayback(musicId: String) {
        viewModelScope.launch { toggleMusicPlaybackUseCase(musicId) }
    }
}
