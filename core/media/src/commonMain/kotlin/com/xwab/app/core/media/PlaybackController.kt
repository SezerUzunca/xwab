package com.xwab.app.core.media

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
interface PlaybackController {

    val state: StateFlow<AudioPlayerState>

    /** The active sleep timer, kept separate so non-player screens do not refresh every second. */
    val sleepTimerState: StateFlow<SleepTimerState>

    /** Submit a user command. Platform-specific pre-processing happens before dispatch. */
    fun submit(command: PlaybackCommand)

    /**
     * Release client-side resources. Owned by DI (`onClose`), not by application code.
     * On Android this only disconnects the client; the PlaybackService keeps running.
     */
    fun release()
}
