package com.xwab.app.core.playbackengine.port

/** [uri] is an HTTPS URI or an absolute local path interpreted by the platform playback adapter. */
public data class AudioSource(
    public val id: String,
    public val uri: String,
    public val title: String? = null,
    public val artist: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Audio source id cannot be blank." }
        require(uri.isNotBlank()) { "Audio source reference cannot be blank." }
    }
}

public enum class LoopMode {
    Off,
    One,
}

public data class PlaybackRequest(
    public val source: AudioSource,
    public val autoplay: Boolean = false,
    public val loopMode: LoopMode = LoopMode.Off,
    public val volume: Float = 1.0f,
) {
    init {
        require(volume in 0.0f..1.0f) { "Volume must be between 0.0 and 1.0." }
    }
}

public enum class PlaybackPhase {
    Idle,
    Loading,
    Buffering,
    Ready,
    Ended,
    Failed,
}

public enum class PlaybackErrorCode {
    InvalidSource,
    ServiceUnavailable,
    Timeout,
    PlaybackFailed,
}

public data class PlaybackError(
    public val code: PlaybackErrorCode,
    public val message: String? = null,
)

/**
 * A timer belongs to playback, but is intentionally exposed separately from [AudioPlayerState].
 * The latter is consumed by multiple screens and must not emit once per second just for the timer UI.
 */
public data class SleepTimerState(
    public val remainingMs: Long? = null,
) {
    init {
        require(remainingMs == null || remainingMs >= 0L) {
            "Sleep timer remaining time cannot be negative."
        }
    }
}

public data class AudioPlayerState(
    /** The source most recently requested by the app, including during reconnects. */
    public val requestedSource: AudioSource? = null,
    /** The source currently attached to the native playback engine, if any. */
    public val source: AudioSource? = null,
    public val phase: PlaybackPhase = PlaybackPhase.Idle,
    /** Whether playback is desired, even while the native engine is not actively playing yet. */
    public val playRequested: Boolean = false,
    public val isPlaying: Boolean = false,
    public val isLooping: Boolean = false,
    public val volume: Float = 1.0f,
    public val error: PlaybackError? = null,
) {
    /** The source consumers should render while the engine is reconnecting. */
    public val activeSource: AudioSource?
        get() = source ?: requestedSource
}
