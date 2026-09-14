package com.xwab.app.feature.sound

import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.session.port.DEFAULT_LOOPING

internal enum class SoundError {
    SoundNotFound,
    SoundCouldNotOpen,
    SoundUnavailable,
}

/** Content available after the outer [com.xwab.app.designsystem.state.Loadable] becomes ready. */
internal data class SoundState(
    val track: Track? = null,
    val isFavorite: Boolean = false,
    /**
     * What the play/pause control shows, and what a tap on it branches on.
     *
     * The session's intent, not whether sound is audible: those differ while a source is being
     * resolved or buffered, and drawing one while acting on the other is how a tap during buffering
     * used to pause a sound the screen was still showing as stopped.
     */
    val playIntent: Boolean = false,
    /** Playback is wanted but not audible yet. */
    val isPreparing: Boolean = false,
    val isLooping: Boolean = DEFAULT_LOOPING,
    val volume: Float = 1.0f,
    val sleepTimerRemainingMs: Long? = null,
    val error: SoundError? = null,
)
