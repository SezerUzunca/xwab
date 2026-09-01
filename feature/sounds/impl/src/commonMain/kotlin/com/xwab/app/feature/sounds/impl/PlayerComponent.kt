package com.xwab.app.feature.sounds.impl

import com.arkivanov.decompose.ComponentContext
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.navigation.componentScope
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackFailure
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.ui.state.Loadable
import com.xwab.app.feature.sounds.impl.domain.ObservePlayerContentUseCase
import com.xwab.app.feature.sounds.impl.domain.PlayerContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface PlayerComponent {
    val state: StateFlow<Loadable<PlayerState>>
    val onBack: () -> Unit
    fun toggleFavorite()
    fun togglePlayback()
    fun setLooping(enabled: Boolean)
    fun setVolume(volume: Float)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
}

internal class DefaultPlayerComponent(
    componentContext: ComponentContext,
    private val trackId: TrackId,
    observePlayerContentUseCase: ObservePlayerContentUseCase,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
    override val onBack: () -> Unit,
) : PlayerComponent, ComponentContext by componentContext {
    private val scope = componentScope()

    /** This screen is about one sound, so that is the item it recognises in the session. */
    private val itemId = PlaybackItemId.sound(trackId.value)

    override val state: StateFlow<Loadable<PlayerState>> = observePlayerContentUseCase(trackId)
        .map<PlayerContent, Loadable<PlayerState>> { content ->
            val playback = content.playback
            val isRequested = playback.requestedItemId == itemId
            // Bound locally: `failure` is another module's property, so the null check below cannot
            // smart-cast it in place.
            val failure = playback.failure
            Loadable.Ready(
                PlayerState(
                    music = content.music,
                    isFavorite = trackId in content.favoriteIds,
                    playIntent = isRequested && playback.playIntent,
                    isPreparing = isRequested && playback.isPreparing,
                    // Straight from the session, including before anything is loaded: the product
                    // default lives there, so this screen has no second opinion to disagree with it.
                    isLooping = playback.isLooping,
                    volume = playback.volume,
                    sleepTimerRemainingMs = content.sleepTimerRemainingMs,
                    error = when {
                        content.music == null -> PlayerError.AudioNotFound
                        // Matched against the failure's own track, not the session's current one. A
                        // lookup that fails releases its claim, so the session has already fallen
                        // back to whatever was playing before — gating on that hid every resolution
                        // error this screen caused.
                        failure != null && failure.itemId == itemId -> failure.asPlayerError()
                        else -> null
                    },
                ),
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )

    override fun toggleFavorite() {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.music == null) return
        scope.launch { favoritesRepository.toggle(trackId) }
    }

    /**
     * Branches on the same value the control renders, which is the whole point of the session
     * publishing an intent: whatever the icon says, the tap does.
     */
    override fun togglePlayback() {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.playIntent) {
            playbackCoordinator.pause()
        } else {
            scope.launch { playbackCoordinator.play(itemId) }
        }
    }

    /**
     * The four settings below reach the coordinator unchanged. They used to go through a use case
     * each, and none of those held a decision — a use case has to earn its name.
     */
    override fun setLooping(enabled: Boolean) = playbackCoordinator.setLooping(enabled)

    override fun setVolume(volume: Float) = playbackCoordinator.setVolume(volume)

    override fun startSleepTimer(durationMs: Long) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.music != null) playbackCoordinator.startSleepTimer(durationMs)
    }

    override fun cancelSleepTimer() = playbackCoordinator.cancelSleepTimer()
}

/**
 * A missing track and an unreachable one read the same on screen otherwise, and they are not the
 * same advice: one is a dead end, the other is worth another tap.
 */
private fun PlaybackFailure.asPlayerError(): PlayerError = when (this) {
    is PlaybackFailure.ItemNotFound -> PlayerError.AudioNotFound
    is PlaybackFailure.SourceUnavailable -> PlayerError.AudioUnavailable
    is PlaybackFailure.EngineFailed -> PlayerError.AudioCouldNotOpen
}
