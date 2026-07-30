package com.xwab.app.core.media

/**
 * The single mailbox message type consumed by [reducePlayback].
 *
 * Messages are either user commands (Load/Play/… — issued on the user's behalf,
 * ultimately from a public [PlaybackCommand]) or runtime events (native engine
 * callbacks and Android controller lifecycle). This is an INTERNAL type: the
 * public surface accepts only [PlaybackCommand], keeping runtime-only events out.
 */
internal sealed interface PlaybackMessage {

    // ── User commands ────────────────────────────────────────────────────

    data class Load(val request: PlaybackRequest) : PlaybackMessage
    data object Play : PlaybackMessage
    data object Pause : PlaybackMessage
    data class SetLooping(val enabled: Boolean) : PlaybackMessage
    data class SetVolume(val volume: Float) : PlaybackMessage

    /** A validated deadline expressed on the current platform's monotonic clock. */
    data class StartSleepTimer(
        val deadlineElapsedRealMs: Long,
    ) : PlaybackMessage

    data object CancelSleepTimer : PlaybackMessage
    data object Release : PlaybackMessage

    // ── Engine events (both platforms) ───────────────────────────────────

    /** An engine event tied to one source operation. */
    sealed interface SourceOperationEvent : PlaybackMessage {
        val operationId: Long
    }

    /** The native engine successfully attached the requested source. */
    data class EngineSourceLoaded(
        override val operationId: Long,
        val source: AudioSource,
    ) : SourceOperationEvent

    /** The native engine reached the end of the current item. */
    data class EnginePlaybackEnded(
        override val operationId: Long,
    ) : SourceOperationEvent

    /** A fatal playback error was reported by the native engine. */
    data class EngineFailed(
        override val operationId: Long,
        val error: PlaybackError,
    ) : SourceOperationEvent

    /** Audio session was interrupted (e.g. phone call). Non-fatal soft pause. */
    data object EngineInterrupted : PlaybackMessage

    /**
     * The native engine's observable playback settings changed.
     *
     * Android uses this to acknowledge applied overrides and to adopt changes
     * coming from MediaSession controls instead of replaying stale commands
     * after a reconnect.
     */
    data class EnginePlaybackObserved(
        override val operationId: Long,
        val source: AudioSource?,
        val playWhenReady: Boolean,
        val looping: Boolean,
        val volume: Float,
    ) : SourceOperationEvent

    /** The sleep timer expired — playback should stop. */
    data object SleepTimerExpired : PlaybackMessage

    /** Android service confirmed the currently owned timer deadline. */
    data class SleepTimerDeadlineObserved(
        val deadlineElapsedRealMs: Long?,
    ) : PlaybackMessage

    // ── Android controller lifecycle ────────────────────────────────────

    /**
     * The MediaController has connected to the PlaybackService.
     *
     * Values reflect what the service currently owns; the reducer decides
     * whether to load a pending request or adopt the service state.
     */
    data class ControllerConnected(
        val attachedSource: AudioSource?,
        val attachedOperationId: Long? = null,
        val attachedOperationOwnedByClient: Boolean = false,
        val controllerLooping: Boolean,
        val controllerVolume: Float,
        val controllerPlayWhenReady: Boolean,
    ) : PlaybackMessage

    /** The MediaController disconnected. */
    data object ControllerDisconnected : PlaybackMessage

    /** Unable to connect to the PlaybackService at all. */
    data object ControllerConnectionFailed : PlaybackMessage
}

/**
 * The platform-independent command → mailbox message mapping.
 *
 * A facade only overrides the commands that need native pre-processing (Android
 * computes a monotonic sleep-timer deadline and drives its UI ticker); everything
 * else routes through here so a new command is mapped in one place.
 */
internal fun PlaybackCommand.toMessage(
    sleepTimerDeadlineElapsedRealMs: Long? = null,
): PlaybackMessage = when (this) {
    is PlaybackCommand.Load -> PlaybackMessage.Load(request)
    PlaybackCommand.Play -> PlaybackMessage.Play
    PlaybackCommand.Pause -> PlaybackMessage.Pause
    is PlaybackCommand.SetLooping -> PlaybackMessage.SetLooping(enabled)
    is PlaybackCommand.SetVolume -> PlaybackMessage.SetVolume(volume)
    // No deadline: platforms that own an out-of-process timer compute one themselves.
    is PlaybackCommand.StartSleepTimer -> PlaybackMessage.StartSleepTimer(
        requireNotNull(sleepTimerDeadlineElapsedRealMs) {
            "Sleep timer commands require a validated platform-monotonic deadline."
        },
    )
    PlaybackCommand.CancelSleepTimer -> PlaybackMessage.CancelSleepTimer
}
