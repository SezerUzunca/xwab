package com.xwab.app.core.playback

import com.xwab.app.core.audiocontent.AudioContentResolver
import com.xwab.app.core.model.PlaybackSummary
import com.xwab.app.core.media.api.AudioPlayerState
import com.xwab.app.core.media.api.AudioSource
import com.xwab.app.core.media.api.LoopMode
import com.xwab.app.core.media.api.PlaybackCommand
import com.xwab.app.core.media.api.PlaybackController
import com.xwab.app.core.media.api.PlaybackPhase
import com.xwab.app.core.media.api.PlaybackRequest
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultPlaybackCoordinator(
    private val controller: PlaybackController,
    private val contentResolver: AudioContentResolver,
) : PlaybackCoordinator {
    /**
     * Mapping to the domain summary here, rather than in a use case, is what keeps the engine's
     * state model inside this module. `map` over a StateFlow still hands every new collector the
     * current value, so nothing downstream has to wait for the next engine update.
     */
    override val playback: Flow<PlaybackSummary> = controller.state.map { it.toPlaybackSummary() }
    override val sleepTimerRemainingMs: Flow<Long?> = controller.sleepTimerState.map { it.remainingMs }

    /**
     * The controller owns loop and volume: it reconciles them across the engine, a remote
     * controller and reconnects, then publishes the result. The one thing it cannot know is
     * that sleep sounds are meant to loop — its own default is "no loop". This flag marks the
     * point where that product default stops applying because a real preference exists to read.
     */
    private var loopPreferenceEstablished = false
    private val sourceResolutionMutex = Mutex()
    private var latestSourceRequest = 0L

    override suspend fun togglePlayback(music: Music) {
        // Every user action invalidates an older source lookup, including a pause/play action on
        // the currently active track while another track is still being resolved.
        val request = sourceResolutionMutex.withLock {
            latestSourceRequest += 1
            latestSourceRequest
        }
        val current = controller.state.value
        when {
            current.activeSource?.id != music.id || current.phase == PlaybackPhase.Failed -> {
                val resolvedUri = contentResolver.resolve(music.id) ?: return
                sourceResolutionMutex.withLock {
                    if (request == latestSourceRequest) {
                        val stateAtLoad = controller.state.value
                        controller.submit(PlaybackCommand.Load(music.toPlaybackRequest(stateAtLoad, resolvedUri)))
                    }
                }
            }
            current.playRequested -> controller.submit(PlaybackCommand.Pause)
            else -> controller.submit(PlaybackCommand.Play)
        }
    }

    override fun setLooping(enabled: Boolean) {
        loopPreferenceEstablished = true
        controller.submit(PlaybackCommand.SetLooping(enabled))
    }

    override fun setVolume(volume: Float) {
        require(volume.isFinite()) { "Volume must be finite." }
        controller.submit(PlaybackCommand.SetVolume(volume.coerceIn(0.0f, 1.0f)))
    }

    override fun startSleepTimer(durationMs: Long) {
        controller.submit(PlaybackCommand.StartSleepTimer(durationMs))
    }

    override fun cancelSleepTimer() {
        controller.submit(PlaybackCommand.CancelSleepTimer)
    }

    private fun Music.toPlaybackRequest(current: AudioPlayerState, resolvedUri: String): PlaybackRequest = PlaybackRequest(
        source = AudioSource(id, resolvedUri, playbackTitle, playbackArtist),
        autoplay = true,
        loopMode = current.loopModeToCarryOver(),
        volume = current.volume,
    )

    /**
     * Carries the session's own loop setting into the next sound whenever one exists.
     *
     * The test is [AudioPlayerState.activeSource], not `source`: a dropped service connection
     * clears only the *attached* source while the session and its reconciled settings live on,
     * so keying off `source` would silently undo the user's choice on the next sound.
     */
    private fun AudioPlayerState.loopModeToCarryOver(): LoopMode = when {
        loopPreferenceEstablished || activeSource != null ->
            if (isLooping) LoopMode.One else LoopMode.Off
        else -> DEFAULT_LOOP_MODE
    }

    private fun AudioPlayerState.toPlaybackSummary(): PlaybackSummary = PlaybackSummary(
        activeSourceId = activeSource?.id,
        isPlaying = isPlaying,
        isLooping = isLooping,
        volume = volume,
        hasFailed = phase == PlaybackPhase.Failed,
    )

    private companion object {
        /** Sleep sounds are meant to run all night, so a first sound loops unless told otherwise. */
        val DEFAULT_LOOP_MODE = LoopMode.One
    }
}
