package com.xwab.app.core.playback.port

import kotlinx.coroutines.flow.StateFlow

/**
 * The public playback seam for the application layer.
 *
 * Outer layers observe [state] / [sleepTimerState] and drive playback with a
 * single [submit] entry point. Implementations are the platform facades
 * (Media3 on Android, AVFoundation on iOS), created by this module's DI.
 *
 * Implementations must be created and used exclusively from the main thread.
 */
public interface PlaybackEnginePort {

    public val state: StateFlow<AudioPlayerState>

    /** The active sleep timer, kept separate so non-player screens do not refresh every second. */
    public val sleepTimerState: StateFlow<SleepTimerState>

    /** Submit a user command. Platform-specific pre-processing happens before dispatch. */
    public fun submit(command: PlaybackCommand)

    /**
     * Releases client-side resources owned by this process. On Android this disconnects the
     * client while the playback service may continue running.
     */
    public fun release()
}
