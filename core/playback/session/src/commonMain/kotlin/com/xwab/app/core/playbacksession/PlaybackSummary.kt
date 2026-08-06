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

/**
 * Why the session could not play a track — and *which* track.
 *
 * The id is part of the failure because a lookup that fails releases the session's claim on the
 * track it was for: by the time the failure is published, the session has fallen back to whatever
 * was playing before, or to nothing. A screen asking "is this failure mine?" has to compare against
 * the failure's own track. Gating on the session's current one hid every resolution error.
 */
sealed interface PlaybackFailure {
    val trackId: TrackId

    /** The catalog holds no such track. Tapping again cannot help. */
    data class TrackNotFound(override val trackId: TrackId) : PlaybackFailure

    /** The track exists, but no source could be produced for it. Worth another tap. */
    data class SourceUnavailable(override val trackId: TrackId) : PlaybackFailure

    /** The engine accepted a source and then failed on it. */
    data class EngineFailed(override val trackId: TrackId) : PlaybackFailure
}

/**
 * The playback view the application layer works with.
 *
 * Deliberately engine-independent: features consume this shared projection instead of the
 * playback engine's technical state model.
 *
 * Two track ids, because during a switch they genuinely differ: the listener has asked for B while
 * A is still the sound coming out of the speaker. Collapsing them into one field published "B is
 * playing" for as long as B took to resolve.
 */
data class PlaybackSummary(
    /**
     * The track the listener last asked for.
     *
     * What a screen highlights and what its controls act on — set from the moment of the tap, before
     * any source lookup. `null` when the session has never been given a track.
     */
    val requestedTrackId: TrackId? = null,
    /**
     * The track the engine is actually holding, and therefore the one [isPlaying] describes.
     *
     * Differs from [requestedTrackId] while a switch is in flight, and goes `null` while a dropped
     * service connection is being restored — the session's own choice lives on in [requestedTrackId].
     */
    val activeTrackId: TrackId? = null,
    /**
     * Whether playback is *wanted*.
     *
     * This — not [isPlaying] — is what a play/pause control renders, and what a screen branches on
     * when the listener taps it. The two used to differ: the session decided from the desired state
     * while the screen drew the actual one, so during buffering a listener saw a Play icon and got
     * a pause out of tapping it.
     */
    val playIntent: Boolean = false,
    /** Whether the engine is producing sound right now — for [activeTrackId], not for the request. */
    val isPlaying: Boolean = false,
    /** The requested track is wanted but not audible yet: being resolved, loaded or buffered. */
    val isPreparing: Boolean = false,
    val isLooping: Boolean = DEFAULT_LOOPING,
    val volume: Float = 1.0f,
    val failure: PlaybackFailure? = null,
)
