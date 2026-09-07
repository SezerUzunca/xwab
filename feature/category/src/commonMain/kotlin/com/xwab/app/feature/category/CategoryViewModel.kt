package com.xwab.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.playback.port.PlaybackPort
import com.xwab.app.core.playback.port.PlaybackItemId
import com.xwab.app.core.playback.port.PlaybackKind
import com.xwab.app.core.playback.port.requestedValueOf
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.feature.category.domain.CategoryContent
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CategoryViewModel(
    categoryId: CategoryId,
    observeCategoryContentUseCase: ObserveCategoryContentUseCase,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<Loadable<CategoryState>> = observeCategoryContentUseCase(categoryId)
        .map<CategoryContent, Loadable<CategoryState>> { content ->
            Loadable.Ready(CategoryState(
                category = content.category,
                musics = content.musics,
                favoriteIds = content.favoriteIds,
                // A story occupying the session lights up no row on a screen that lists sounds.
                requestedTrackId = content.playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId),
                playIntent = content.playback.playIntent,
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )

    fun toggleFavorite(musicId: TrackId) {
        viewModelScope.launch { favoritesPort.toggle(musicId) }
    }

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(musicId: TrackId) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.requestedTrackId == musicId && current.playIntent) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId.sound(musicId.value)) }
        }
    }
}
