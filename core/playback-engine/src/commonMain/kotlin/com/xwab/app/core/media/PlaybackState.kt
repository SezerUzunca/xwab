package com.xwab.app.core.media

/**
 * Internal state owned by the [reducePlayback] function.
 *
 * This is NOT the published UI state — [AudioPlayerState] is. Each platform
 * driver derives the published state by combining this model with real-time
 * native engine values (phase, isPlaying).
 *
 * The model separates three concerns:
 *
 * - [desired] — what the user (or autoplay) wants: the requested source and
 *   the loop/volume/play/ended lifecycle.
 * - [observed] — what the reducer currently knows the native engine holds: the
 *   attached source and the last reported error. (Live values such as phase and
 *   isPlaying are still read directly from the engine at projection time.)
 * - [pending] — in-flight reconciliation: the operation generation and the
 *   overrides that are awaiting acknowledgement from the engine/controller.
 */
internal data class PlaybackState(
    val desired: DesiredPlayback = DesiredPlayback(),
    val observed: ObservedPlayback = ObservedPlayback(),
    val pending: PendingReconciliation = PendingReconciliation(),
    val sleepTimerLifecycle: SleepTimerLifecycle = SleepTimerLifecycle.Unchanged,
    val released: Boolean = false,
)

/** What the user (or autoplay) wants playback to be doing. */
internal data class DesiredPlayback(
    /** The source most recently requested by the app, including during reconnects. */
    val request: PlaybackRequest? = null,
    val isLooping: Boolean = false,
    val volume: Float = 1.0f,
    /** Whether playback is desired, even while the native engine is not actively playing yet. */
    val playRequested: Boolean = false,
    /** Whether the current source reached its natural end (non-looping). */
    val ended: Boolean = false,
)

/** What the reducer currently knows the native engine holds. */
internal data class ObservedPlayback(
    /** The source currently attached to the native playback engine, if any. */
    val source: AudioSource? = null,
    val error: PlaybackError? = null,
)

/** In-flight reconciliation between desired and observed state. */
internal data class PendingReconciliation(
    /** Identifies the latest source operation so superseded native callbacks can be ignored. */
    val operationId: Long = 0L,
    /** The source operation that has not yet been applied to the native engine. */
    val pendingSourceOperationId: Long? = null,

    // Null means "we have not overridden this; adopt the controller's value".
    val pendingPlayWhenReady: Boolean? = null,
    val pendingLooping: Boolean? = null,
    val pendingVolume: Float? = null,
    /** A deferred stop-and-rewind that should be applied on reconnect. */
    val rewindRequested: Boolean = false,
    /** Prevents repeated reload effects while recovering from an unexpected native source. */
    val sourceRecoveryInFlight: Boolean = false,
)

/**
 * Tracks the client-side intention for the sleep timer across reconnects.
 *
 * [Unchanged] means the facade has not issued any timer command yet and
 * the running service's state (if any) should be adopted.
 */
internal sealed interface SleepTimerLifecycle {
    data object Unchanged : SleepTimerLifecycle
    data object Cancelled : SleepTimerLifecycle
    data class Running(val deadlineElapsedRealMs: Long) : SleepTimerLifecycle {
        init {
            require(deadlineElapsedRealMs > 0L) { "Sleep timer deadline must be positive." }
        }
    }
}

/** Returns the remaining time until [deadlineMs], or null if already expired. */
internal fun remainingDurationUntil(deadlineMs: Long, nowMs: Long): Long? {
    if (deadlineMs <= 0L || nowMs < 0L || deadlineMs <= nowMs) return null
    return deadlineMs - nowMs
}

/** Shared deadline for a source to become playable on either native engine. */
internal const val PLAYBACK_READINESS_TIMEOUT_MS = 15_000L

/**
 * Turns a requested sleep-timer duration into a deadline on the caller's monotonic
 * clock, rejecting inputs that cannot produce a usable one.
 */
internal fun sleepTimerDeadline(nowMs: Long, durationMs: Long): Long {
    require(nowMs >= 0L) { "Monotonic clock reading cannot be negative." }
    require(durationMs > 0L) { "Sleep timer duration must be positive." }
    require(durationMs <= Long.MAX_VALUE - nowMs) {
        "Sleep timer deadline exceeds the supported range."
    }
    return nowMs + durationMs
}

/**
 * Computes the canonical [PlaybackPhase] from reducer state and normalized
 * native-engine flags. Both platform adapters map their native states here.
 */
internal fun playbackPhase(
    error: PlaybackError?,
    ended: Boolean,
    hasCurrentItem: Boolean,
    isReadyToPlay: Boolean,
    isWaitingToPlay: Boolean,
): PlaybackPhase = when {
    error != null -> PlaybackPhase.Failed
    !hasCurrentItem -> PlaybackPhase.Idle
    ended -> PlaybackPhase.Ended
    !isReadyToPlay -> PlaybackPhase.Loading
    isWaitingToPlay -> PlaybackPhase.Buffering
    else -> PlaybackPhase.Ready
}
