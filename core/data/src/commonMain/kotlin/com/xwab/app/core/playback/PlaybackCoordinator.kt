package com.xwab.app.core.playback

import com.xwab.app.core.media.AudioPlayerState
import com.xwab.app.core.media.AudioSource
import com.xwab.app.core.media.LoopMode
import com.xwab.app.core.media.PlaybackCommand
import com.xwab.app.core.media.PlaybackController
import com.xwab.app.core.media.PlaybackPhase
import com.xwab.app.core.media.PlaybackRequest
import com.xwab.app.core.media.SleepTimerState
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import xwab.core.data.generated.resources.Res

interface PlaybackCoordinator {
    val state: StateFlow<AudioPlayerState>
    val sleepTimerState: StateFlow<SleepTimerState>

    fun toggle(music: Music)
    fun setLooping(enabled: Boolean)
    fun setVolume(volume: Float)
    fun startSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
}

internal class DefaultPlaybackCoordinator(
    private val controller: PlaybackController,
    private val audioUriForResource: (String) -> String = Res::getUri,
) : PlaybackCoordinator {
    override val state: StateFlow<AudioPlayerState> = controller.state
    override val sleepTimerState: StateFlow<SleepTimerState> = controller.sleepTimerState

    /**
     * Session preference used only before a source is attached: it seeds the product
     * default (looping sleep sounds) and remembers a choice made before the first load.
     * Once a source is attached, the player state is the single source of truth — so a
     * change applied by the engine or a remote controller is adopted, not overwritten.
     */
    private var loopMode: LoopMode = LoopMode.One
    private var volume: Float = 1.0f

    @OptIn(ExperimentalResourceApi::class)
    override fun toggle(music: Music) {
        val current = state.value
        when {
            current.activeSource?.id != music.id || current.phase == PlaybackPhase.Failed ->
                controller.submit(PlaybackCommand.Load(music.toPlaybackRequest(autoplay = true)))
            current.playRequested -> controller.submit(PlaybackCommand.Pause)
            else -> controller.submit(PlaybackCommand.Play)
        }
    }

    override fun setLooping(enabled: Boolean) {
        loopMode = if (enabled) LoopMode.One else LoopMode.Off
        controller.submit(PlaybackCommand.SetLooping(enabled))
    }

    override fun setVolume(volume: Float) {
        require(volume.isFinite()) { "Volume must be finite." }
        val safeVolume = volume.coerceIn(0.0f, 1.0f)
        this.volume = safeVolume
        controller.submit(PlaybackCommand.SetVolume(safeVolume))
    }

    override fun startSleepTimer(durationMs: Long) {
        controller.submit(PlaybackCommand.StartSleepTimer(durationMs))
    }

    override fun cancelSleepTimer() {
        controller.submit(PlaybackCommand.CancelSleepTimer)
    }

    private fun Music.toPlaybackRequest(autoplay: Boolean): PlaybackRequest {
        val current = state.value
        // Adopt the attached source's live settings (which reflect engine/remote changes);
        // fall back to the session preference only before any source is attached.
        val attached = current.source != null
        return PlaybackRequest(
            source = AudioSource(id, audioUriForResource(audioResource), playbackTitle, playbackArtist),
            autoplay = autoplay,
            loopMode = if (attached) {
                if (current.isLooping) LoopMode.One else LoopMode.Off
            } else {
                loopMode
            },
            volume = if (attached) current.volume else volume,
        )
    }
}
