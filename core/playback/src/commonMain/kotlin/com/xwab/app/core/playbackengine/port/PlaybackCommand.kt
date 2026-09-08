package com.xwab.app.core.playbackengine.port

/**
 * The public playback vocabulary an application submits through
 * [PlaybackEnginePort.submit].
 *
 * This is the only playback input the outer layers can express. Runtime-only
 * concerns (native engine events, controller lifecycle) live in the internal
 * engine-message type and can never be fabricated from here. Lifecycle
 * teardown (`release`) is intentionally absent — it is a DI/ownership concern,
 * not a user command.
 */
public sealed interface PlaybackCommand {

    /** Replace the current source using one atomic playback configuration. */
    public data class Load(public val request: PlaybackRequest) : PlaybackCommand

    public data object Play : PlaybackCommand
    public data object Pause : PlaybackCommand

    public data class SetLooping(public val enabled: Boolean) : PlaybackCommand

    /** Set the audio volume between 0.0 (mute) and 1.0 (max). */
    public data class SetVolume(public val volume: Float) : PlaybackCommand

    /** Stop playback when the requested duration has elapsed. */
    public data class StartSleepTimer(public val durationMs: Long) : PlaybackCommand

    /** Clear an active sleep timer without changing playback. */
    public data object CancelSleepTimer : PlaybackCommand
}
