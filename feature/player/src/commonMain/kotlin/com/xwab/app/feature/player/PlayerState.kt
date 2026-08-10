package com.xwab.app.feature.player

import com.xwab.app.core.catalog.Music
import com.xwab.app.core.playbacksession.DEFAULT_LOOPING

internal enum class PlayerError {
    AudioNotFound,
    AudioCouldNotOpen,
    AudioUnavailable,
}

internal data class PlayerState(
    val music: Music? = null,
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
    val error: PlayerError? = null,
)
