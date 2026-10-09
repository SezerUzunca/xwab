package com.xwab.app.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.requestedValueOf
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.feature.favorites.domain.FavoritesContent
import com.xwab.app.feature.favorites.domain.ObserveFavoritesContentUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** How long after a removal the list may offer Undo again when it is shown anew. */
private val UNDO_OFFER_WINDOW = 10.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class FavoritesViewModel(
    observeFavoritesContentUseCase: ObserveFavoritesContentUseCase,
    private val playbackPort: PlaybackPort,
    private val favoritesPort: FavoritesPort,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    /** When the pending removal happened; the Undo offer is only repeated shortly after it. */
    private var removalMark: TimeMark? = null
    // The port's per-collection fallback may be empty after an upstream restart.
    private var lastKnownTracks: List<Track>? = null
    private val favoriteWriteFailed = MutableStateFlow(false)
    private val lastRemoval = MutableStateFlow<Track?>(null)
    val removedTrack = lastRemoval.asStateFlow()
    private val failedUndo = MutableStateFlow(false)
    val undoFailed = failedUndo.asStateFlow()
    private val favoritesMutex = Mutex()

    val state: StateFlow<FavoritesUiState> = combine<FavoritesContent, Boolean, FavoritesUiState>(
        observeFavoritesContentUseCase(), favoriteWriteFailed,
    ) { content, writeFailed ->
            if (content.favoritesAvailable) lastKnownTracks = content.tracks
            val tracks = lastKnownTracks ?: content.tracks
            val playback = content.playback
            val requestedTrackId = playback.requestedValueOf(SOUND_PLAYBACK_KIND)?.let(::TrackId)
                ?.takeIf { id -> tracks.any { it.id == id } }
            val failure = playback.failure?.takeIf { failure ->
                failure.itemId.kind == SOUND_PLAYBACK_KIND && tracks.any { it.id.value == failure.itemId.value }
            }
            FavoritesUiState.Ready(
                FavoritesState(
                    tracks = tracks,
                    favoritesAvailable = content.favoritesAvailable,
                    favoriteWriteFailed = writeFailed,
                    requestedTrackId = requestedTrackId,
                    playIntent = requestedTrackId != null && playback.playIntent,
                    isPreparing = requestedTrackId != null && playback.isPreparing,
                    playbackFailure = failure,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading,
        )

    fun togglePlayback(trackId: TrackId) {
        val current = (state.value as? FavoritesUiState.Ready)?.value ?: return
        if (current.isRowPlaying(trackId)) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId(SOUND_PLAYBACK_KIND, trackId.value)) }
        }
    }

    fun removeFavorite(trackId: TrackId) {
        val current = (state.value as? FavoritesUiState.Ready)?.value
            ?.takeIf { it.favoritesAvailable } ?: return
        val track = current.tracks.find { it.id == trackId } ?: return
        viewModelScope.launch {
            favoritesMutex.withLock {
                val snapshot = favoritesPort.observe(SOUND_FAVORITES_NAMESPACE).first()
                if (!snapshot.isAvailable) {
                    favoriteWriteFailed.value = true
                    return@withLock
                }
                // The store owns the atomic change, so another screen cannot turn a removal into an add.
                val failed = favoritesPort.setFavorite(SOUND_FAVORITES_NAMESPACE, trackId.value, false) ==
                    FavoriteToggleResult.Unavailable
                favoriteWriteFailed.value = failed
                if (!failed) {
                    failedUndo.value = false
                    removalMark = timeSource.markNow()
                    lastRemoval.value = track
                }
            }
        }
    }

    fun undoRemoval(trackId: TrackId) {
        if (lastRemoval.value?.id != trackId) return
        viewModelScope.launch {
            favoritesMutex.withLock {
                if (lastRemoval.value?.id != trackId) return@withLock
                val snapshot = favoritesPort.observe(SOUND_FAVORITES_NAMESPACE).first()
                if (lastRemoval.value?.id != trackId) return@withLock
                val failed = !snapshot.isAvailable ||
                    favoritesPort.setFavorite(SOUND_FAVORITES_NAMESPACE, trackId.value, true) ==
                    FavoriteToggleResult.Unavailable
                favoriteWriteFailed.value = failed
                failedUndo.value = failed
                if (!failed) acknowledgeRemoval(trackId)
            }
        }
    }

    /**
     * Whether the screen should show the Undo offer for [trackId] now.
     *
     * The screen asks every time it (re)starts showing the offer. Leaving the list — opening a
     * sound, switching tabs, rotating — cancels the snackbar without an answer, and coming back
     * would otherwise offer Undo again at any later time, even for a sound saved again elsewhere.
     * So a repeat is only offered within [UNDO_OFFER_WINDOW] of the removal and while the sound is
     * still out of favorites; otherwise the offer is dropped. A failed undo is not offered again:
     * the screen shows its retry control instead.
     */
    suspend fun claimUndoOffer(trackId: TrackId): Boolean {
        if (!isUndoPending(trackId)) return false
        val fresh = removalMark?.let { it.elapsedNow() < UNDO_OFFER_WINDOW } == true
        val snapshot = favoritesPort.observe(SOUND_FAVORITES_NAMESPACE).first()
        val stillRemoved = !snapshot.isAvailable || trackId.value !in snapshot.ids
        // Checked again: a newer removal or an undo may have run while the store was being read.
        val pending = isUndoPending(trackId)
        if (pending && !(fresh && stillRemoved)) acknowledgeRemoval(trackId)
        return pending && fresh && stillRemoved
    }

    private fun isUndoPending(trackId: TrackId): Boolean =
        lastRemoval.value?.id == trackId && !failedUndo.value

    fun acknowledgeRemoval(trackId: TrackId) {
        if (lastRemoval.value?.id == trackId) {
            lastRemoval.value = null
            failedUndo.value = false
        }
    }
}
