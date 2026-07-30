package com.xwab.app.core.media

/**
 * Imperative instructions returned by [reducePlayback] alongside the new state.
 *
 * Each platform driver interprets these effects using its own native engine
 * (Media3 on Android, AVFoundation on iOS). The reducer never calls native
 * APIs directly — all mutations go through these effects.
 */
internal sealed interface PlaybackSideEffect {

    // ── Playback engine ─────────────────────────────────────────────────

    /** Prepare and load the given source into the native engine. */
    data class LoadSource(
        val operationId: Long,
        val request: PlaybackRequest,
        val isLooping: Boolean,
    ) : PlaybackSideEffect

    /** Resume or start playback on the native engine. */
    data object Play : PlaybackSideEffect

    /** Pause the native engine. */
    data object Pause : PlaybackSideEffect

    /**
     * Pause caused by an audio-session interruption (e.g. an incoming call).
     *
     * Unlike [Pause], the driver must preserve its interruption state so that
     * playback can resume when the interruption ends. Carrying this distinction
     * as a dedicated effect keeps the decision in the reducer's output instead
     * of an out-of-band mutable flag on the driver.
     */
    data object PauseForInterruption : PlaybackSideEffect

    /** Seek to position zero, then resume playback (used after ended state). */
    data object SeekToStartThenPlay : PlaybackSideEffect

    /** Change the looping mode on the native engine. */
    data class SetLooping(val enabled: Boolean) : PlaybackSideEffect

    /** Change the volume on the native engine. */
    data class SetVolume(val volume: Float) : PlaybackSideEffect

    /** Pause the native engine and rewind to position zero. */
    data object Stop : PlaybackSideEffect

    /** Permanently release all native resources. */
    data object Release : PlaybackSideEffect

    // ── Sleep timer ─────────────────────────────────────────────────────

    /** Start a new sleep timer with the given parameters. */
    data class StartSleepTimer(
        val deadlineElapsedRealMs: Long,
    ) : PlaybackSideEffect

    /** Cancel an active sleep timer. */
    data object CancelSleepTimer : PlaybackSideEffect

    /** Android reconnect: restore the timer state from the running service. */
    data object RestoreSleepTimer : PlaybackSideEffect

    /**
     * Android reconnect: re-apply a previously started timer using its
     * original deadline so that elapsed time is not lost.
     */
    data class ReconnectSleepTimer(
        val deadlineElapsedRealMs: Long,
    ) : PlaybackSideEffect

}
