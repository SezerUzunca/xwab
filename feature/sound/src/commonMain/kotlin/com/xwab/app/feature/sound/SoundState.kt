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
) {
    /**
     * Whether the transport control has anything to act on.
     *
     * Not simply "the catalog holds this sound": if it stops holding one the session is already
     * playing, refusing here would leave audible sound with nothing able to pause it — the same
     * reason cancelling a running sleep timer is never refused either.
     */
    val canPlay: Boolean get() = track != null || playIntent

    /** Nothing to save while the catalog does not hold the sound. */
    val canFavorite: Boolean get() = track != null

    /** Looping, volume and the timer all need a sound to apply to. */
    val canConfigure: Boolean get() = track != null
}
