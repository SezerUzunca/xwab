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
            playIntent = isRequested && playback.playIntent,
            isPreparing = isRequested && playback.isPreparing,
            // Straight from the session, including before anything is loaded: the product default
            // lives there, so this screen has no second opinion to disagree with it.
            isLooping = playback.isLooping,
            // Straight from the session, which states its range on the port and keeps it. This
            // used to be clamped again here, back when the range was not written down anywhere.
            volume = playback.volume,
            sleepTimerRemainingMs = content.sleepTimerRemainingMs,
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
     * The settings below reach the coordinator unchanged. They used to go through a use case each,
     * and none of those held a decision — a use case has to earn its name.
     *
     * Each refuses what [SoundState.canConfigure] refuses, which is also what the panel drawing
     * them renders as disabled — one predicate, read by both. It used to be spelled out in each
     * layer separately, and applied to a different subset in each.
     */
    fun setLooping(enabled: Boolean) {
        if (readyState()?.canConfigure == true) playbackPort.setLooping(enabled)
    }

    fun setVolume(volume: Float) {
        if (readyState()?.canConfigure == true) playbackPort.setVolume(volume)
    }

    fun startSleepTimer(durationMs: Long) {
        if (readyState()?.canConfigure == true) playbackPort.startSleepTimer(durationMs)
    }

    /**
     * Neither guarded here nor disabled on screen: a timer only runs because there was a track to
     * start it with, and a catalog that drops that track afterwards must not leave it running with
     * nothing able to stop it.
     */
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
