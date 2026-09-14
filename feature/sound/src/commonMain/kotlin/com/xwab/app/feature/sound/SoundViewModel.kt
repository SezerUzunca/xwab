package com.xwab.app.feature.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.feature.sound.domain.ObserveSoundContentUseCase
import com.xwab.app.feature.sound.domain.SoundContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SoundViewModel(
    private val trackId: TrackId,
    observeSoundContentUseCase: ObserveSoundContentUseCase,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    /** This screen is about one sound, so that is the item it recognises in the session. */
    private val itemId = PlaybackItemId.sound(trackId.value)

    val state: StateFlow<Loadable<SoundState>> = observeSoundContentUseCase(trackId)
        .map<SoundContent, Loadable<SoundState>> { content ->
        val playback = content.playback
        val isRequested = playback.requestedItemId == itemId
        // Bound locally: `failure` is another module's property, so the null check below cannot
        // smart-cast it in place.
        val failure = playback.failure
        Loadable.Ready(SoundState(
            track = content.track,
            isFavorite = trackId in content.favoriteIds,
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
        initialValue = Loadable.Loading,
    )

    fun toggleFavorite() {
        if (loadedTrack() == null) return
        viewModelScope.launch { favoritesPort.toggle(SOUND_FAVORITES_NAMESPACE, trackId.value) }
    }

    /**
     * Branches on the same value the control renders, which is the whole point of the session
     * publishing an intent: whatever the icon says, the tap does.
     */
    fun togglePlayback() {
        val current = (state.value as? Loadable.Ready)?.value ?: return
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
     * Each refuses on a track that does not exist, which is the answer the panel drawing them
     * already renders as disabled. The rule used to be stated in both places and applied to a
     * different subset in each: the panel disabled all three controls, while only the sleep timer
     * refused to act.
     */
    fun setLooping(enabled: Boolean) {
        if (loadedTrack() != null) playbackPort.setLooping(enabled)
    }

    fun setVolume(volume: Float) {
        if (loadedTrack() != null) playbackPort.setVolume(volume)
    }

    fun startSleepTimer(durationMs: Long) {
        if (loadedTrack() != null) playbackPort.startSleepTimer(durationMs)
    }

    /**
     * Neither guarded here nor disabled on screen: a timer only runs because there was a track to
     * start it with, and a catalog that drops that track afterwards must not leave it running with
     * nothing able to stop it.
     */
    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()

    /** The track this screen is showing, or null while there is nothing to act on. */
    private fun loadedTrack(): Track? = (state.value as? Loadable.Ready)?.value?.track
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
