package com.xwab.app.core.playbacksession

import com.xwab.app.core.catalog.TrackId

/**
 * Sleep sounds are meant to run all night, so a session loops unless it is told otherwise.
 *
 * The one place this default lives. It used to be stated twice — once in the session, deciding what
 * to hand the engine, and once in the player screen, which showed "looping" whenever no source was
 * attached yet. Turning the loop off before the first play made the two disagree.
 */
const val DEFAULT_LOOPING = true

/** Why the session could not play what was asked of it. */
sealed interface PlaybackFailure {
    /** The catalog holds no such track. Tapping again cannot help. */
    data object TrackNotFound : PlaybackFailure

    /** The track exists, but no source could be produced for it. Worth another tap. */
    data object SourceUnavailable : PlaybackFailure

    /** The engine accepted a source and then failed on it. */
    data object EngineFailed : PlaybackFailure
}

/**
 * The playback view the application layer works with.
 *
 * Deliberately engine-independent: features consume this shared projection instead of the
 * playback engine's technical state model.
 */
data class PlaybackSummary(
    /** The track the session is on, whether or not the engine has been handed its audio yet. */
    val trackId: TrackId? = null,
    /**
     * Whether playback is *wanted*.
     *
     * This — not [isPlaying] — is what a play/pause control renders, and what a screen branches on
     * when the listener taps it. The two used to differ: the session decided from the desired state
     * while the screen drew the actual one, so during buffering a listener saw a Play icon and got
     * a pause out of tapping it.
     */
    val playIntent: Boolean = false,
    /** Whether the engine is producing sound right now. */
    val isPlaying: Boolean = false,
    /** Playback is wanted but not audible yet: a source is being resolved, loaded or buffered. */
    val isPreparing: Boolean = false,
    val isLooping: Boolean = DEFAULT_LOOPING,
    val volume: Float = 1.0f,
    val failure: PlaybackFailure? = null,
)
