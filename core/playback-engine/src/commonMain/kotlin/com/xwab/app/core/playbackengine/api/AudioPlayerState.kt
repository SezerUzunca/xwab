package com.xwab.app.core.playbackengine.api

data class AudioSource(
    val id: String,
    val uri: String,
    val title: String? = null,
    val artist: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Audio source id cannot be blank." }
        require(uri.isNotBlank()) { "Audio source URI cannot be blank." }
    }
}

enum class LoopMode {
    Off,
    One,
}

data class PlaybackRequest(
    val source: AudioSource,
    val autoplay: Boolean = false,
    val loopMode: LoopMode = LoopMode.Off,
    val volume: Float = 1.0f,
) {
    init {
        require(volume in 0.0f..1.0f) { "Volume must be between 0.0 and 1.0." }
    }
}

enum class PlaybackPhase {
    Idle,
    Loading,
    Buffering,
    Ready,
    Ended,
    Failed,
}

enum class PlaybackErrorCode {
    InvalidSource,
    ServiceUnavailable,
    Timeout,
    PlaybackFailed,
}

data class PlaybackError(
    val code: PlaybackErrorCode,
    val message: String? = null,
)

/**
 * A timer belongs to playback, but is intentionally exposed separately from [AudioPlayerState].
 * The latter is consumed by multiple screens and must not emit once per second just for the timer UI.
 */
data class SleepTimerState(
    val remainingMs: Long? = null,
) {
    init {
        require(remainingMs == null || remainingMs >= 0L) {
            "Sleep timer remaining time cannot be negative."
        }
    }
}

data class AudioPlayerState(
    /** The source most recently requested by the app, including during reconnects. */
    val requestedSource: AudioSource? = null,
    /** The source currently attached to the native playback engine, if any. */
    val source: AudioSource? = null,
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    /** Whether playback is desired, even while the native engine is not actively playing yet. */
    val playRequested: Boolean = false,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = false,
    val volume: Float = 1.0f,
    val error: PlaybackError? = null,
) {
    /** The source consumers should render while the engine is reconnecting. */
    val activeSource: AudioSource?
        get() = source ?: requestedSource
}
