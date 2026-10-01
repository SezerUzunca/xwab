package com.xwab.app.feature.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.feature.sound.domain.ObserveSoundContentUseCase
import com.xwab.app.feature.sound.domain.SoundContent
import com.xwab.app.feature.sound.domain.SoundFavoriteReadStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SoundViewModel(
    private val trackId: TrackId,
    observeSoundContentUseCase: ObserveSoundContentUseCase,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    private val favoriteWriteFailed = MutableStateFlow(false)
    // Survives an upstream restart while this ViewModel is still on the back stack.
    private var lastKnownFavorite: Boolean? = null
    /** This screen is about one sound, so that is the item it recognises in the session. */
    private val itemId = PlaybackItemId(SOUND_PLAYBACK_KIND, trackId.value)

    val state: StateFlow<SoundUiState> = combine<SoundContent, Boolean, SoundUiState>(
        observeSoundContentUseCase(trackId), favoriteWriteFailed,
    ) { content, writeFailed ->
        if (content.favoriteReadStatus == SoundFavoriteReadStatus.Available) {
            lastKnownFavorite = content.isFavorite
        }
        val playback = content.playback
        val isRequested = playback.requestedItemId == itemId
        // Bound locally: `failure` is another module's property, so the null check below cannot
        // smart-cast it in place.
        val failure = playback.failure
        SoundUiState.Ready(SoundState(
            track = content.track,
            favoriteReadStatus = content.favoriteReadStatus,
            favoriteWriteFailed = writeFailed,
            isFavorite = lastKnownFavorite ?: content.isFavorite,
            availableOffline = content.availableOffline,
            playIntent = isRequested && playback.playIntent,
            isPreparing = isRequested && playback.isPreparing,
            error = when {
                content.track == null -> SoundError.SoundNotFound
                // Matched against the failure's own track, not the session's current one. A lookup
                // that fails releases its claim, so the session has already fallen back to whatever
                // was playing before — gating on that hid every resolution error this screen caused.
                failure != null && failure.itemId == itemId -> failure.asSoundError()
                else -> null
            },
        ))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundUiState.Loading,
    )

    /**
     * The session's timer. Kept out of [state] on purpose: it ticks every second, and nothing else
     * on this screen should be rebuilt that often.
     */
    val sleepTimerRemainingMs: StateFlow<Long?> = playbackPort.sleepTimerRemainingMs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun toggleFavorite() {
        if (readyState()?.canFavorite != true) return
        viewModelScope.launch {
            favoriteWriteFailed.value = favoritesPort.toggle(SOUND_FAVORITES_NAMESPACE, trackId.value) == FavoriteToggleResult.Unavailable
        }
    }

    /**
     * Branches on the same value the control renders, which is the whole point of the session
     * publishing an intent: whatever the icon says, the tap does.
     */
    fun togglePlayback() {
        val current = readyState() ?: return
        if (!current.canPlay) return
        if (current.playIntent) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(itemId) }
        }
    }

    /**
     * Starts the session's timer and, when this sound is not already playing, plays it: a timer
     * started from a sound's page is a timer for that sound. Restarting it while the sound plays
     * leaves playback alone, and a sound the catalog no longer holds is not started.
     */
    fun startSleepTimer(durationMs: Long) {
        if (durationMs <= 0L) return
        playbackPort.startSleepTimer(durationMs)
        val current = readyState() ?: return
        if (!current.playIntent && current.canPlay) viewModelScope.launch { playbackPort.play(itemId) }
    }

    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()

    /** What the screen is showing, or null while the first content has not arrived. */
    private fun readyState(): SoundState? = (state.value as? SoundUiState.Ready)?.value
}

/**
 * A missing track and an unreachable one read the same on screen otherwise, and they are not the
 * same advice: one is a dead end, the other is worth another tap.
 */
private fun PlaybackFailure.asSoundError(): SoundError = when (this) {
    is PlaybackFailure.ItemNotFound -> SoundError.SoundNotFound
    is PlaybackFailure.SourceUnavailable -> SoundError.SoundUnavailable
    is PlaybackFailure.EngineFailed -> SoundError.SoundCouldNotOpen
}
