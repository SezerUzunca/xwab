package com.xwab.app.core.playbackengine.api

import com.xwab.app.core.playbackengine.store.PlaybackMessage

/**
 * The public playback vocabulary an application submits through
 * [PlaybackController.submit].
 *
 * This is the only playback input the outer layers can express. Runtime-only
 * concerns (native engine events, controller lifecycle) live in the internal
 * `PlaybackMessage` type and can never be fabricated from here. Lifecycle
 * teardown (`release`) is intentionally absent — it is a DI/ownership concern,
 * not a user command.
 */
sealed interface PlaybackCommand {

    /** Replace the current source using one atomic playback configuration. */
    data class Load(val request: PlaybackRequest) : PlaybackCommand

    data object Play : PlaybackCommand
    data object Pause : PlaybackCommand

    data class SetLooping(val enabled: Boolean) : PlaybackCommand

    /** Set the audio volume between 0.0 (mute) and 1.0 (max). */
    data class SetVolume(val volume: Float) : PlaybackCommand

    /** Stop playback when the requested duration has elapsed. */
    data class StartSleepTimer(val durationMs: Long) : PlaybackCommand

    /** Clear an active sleep timer without changing playback. */
    data object CancelSleepTimer : PlaybackCommand
}
