package com.xwab.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackKind
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.feature.category.domain.CategoryContent
import com.xwab.app.feature.category.domain.CategoryFavoritesReadStatus
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CategoryViewModel(
    categoryId: CategoryId,
    observeCategoryContentUseCase: ObserveCategoryContentUseCase,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    private val favoriteWriteFailed = MutableStateFlow(false)
    // Read failures after resubscribing must not clear the last successful membership.
    private var lastKnownFavoriteIds: Set<TrackId>? = null
    val state: StateFlow<CategoryUiState> = combine<CategoryContent, Boolean, CategoryUiState>(
        observeCategoryContentUseCase(categoryId), favoriteWriteFailed,
    ) { content, writeFailed ->
            if (content.favoritesReadStatus == CategoryFavoritesReadStatus.Available) {
                lastKnownFavoriteIds = content.favoriteIds
            }
            val playback = content.playback
            val requestedTrackId = playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId)
                ?.takeIf { id -> content.tracks.any { it.id == id } }
            // Bound locally: `failure` is another module's property, so the check below cannot
            // smart-cast it in place.
            val failure = playback.failure?.takeIf { failure ->
                failure.itemId.kind == PlaybackKind.SOUND &&
                    content.tracks.any { it.id.value == failure.itemId.value }
            }

            CategoryUiState.Ready(CategoryState(
                category = content.category,
                favoritesReadStatus = content.favoritesReadStatus,
                favoriteWriteFailed = writeFailed,
                tracks = content.tracks,
                favoriteIds = lastKnownFavoriteIds?.let { ids ->
                    if (content.favoritesReadStatus == CategoryFavoritesReadStatus.Available) ids
                    else content.tracks.mapNotNullTo(mutableSetOf()) { it.id.takeIf(ids::contains) }
                } ?: content.favoriteIds,
                requestedTrackId = requestedTrackId,
                // Only playback represented by a row belongs in this screen's state.
                playIntent = requestedTrackId != null && playback.playIntent,
                isPreparing = requestedTrackId != null && playback.isPreparing,
                playbackFailure = failure,
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryUiState.Loading,
        )

    fun toggleFavorite(trackId: TrackId) {
        if ((state.value as? CategoryUiState.Ready)?.value?.favoritesAvailable != true) return
        viewModelScope.launch {
            favoriteWriteFailed.value = favoritesPort.toggle(SOUND_FAVORITES_NAMESPACE, trackId.value) == FavoriteToggleResult.Unavailable
        }
    }

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(trackId: TrackId) {
        val current = (state.value as? CategoryUiState.Ready)?.value ?: return
        if (current.isRowPlaying(trackId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId.sound(trackId.value)) }
        }
    }
}
