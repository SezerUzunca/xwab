package com.xwab.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackKind
import com.xwab.app.core.session.port.requestedValueOf
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
            val playback = content.playback
            // A story occupying the session lights up no row on a screen that lists sounds.
            val requestedTrackId = playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId)
            // Bound locally: `failure` is another module's property, so the check below cannot
            // smart-cast it in place.
            val failure = playback.failure?.takeIf { it.itemId.kind == PlaybackKind.SOUND }

            Loadable.Ready(CategoryState(
                category = content.category,
                tracks = content.tracks,
                favoriteIds = content.favoriteIds,
                requestedTrackId = requestedTrackId,
                // Gated on the id for the same reason the other screens gate it: these say "the
                // session is on a sound", and it is not when the session is on a story. Which row
                // that sound is — if it is on this screen at all — is [CategoryState.isRowPlaying].
                playIntent = requestedTrackId != null && playback.playIntent,
                isPreparing = requestedTrackId != null && playback.isPreparing,
                playbackFailure = failure,
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )

    fun toggleFavorite(trackId: TrackId) {
        viewModelScope.launch { favoritesPort.toggle(SOUND_FAVORITES_NAMESPACE, trackId.value) }
    }

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(trackId: TrackId) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.isRowPlaying(trackId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId.sound(trackId.value)) }
        }
    }
}
