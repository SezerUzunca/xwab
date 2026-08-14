package com.xwab.app.core.playbacksession

/**
 * What looping should be before anything has been loaded and before the listener has chosen.
 *
 * The one place this default lives. It used to be stated twice — once in the session, deciding what
 * to hand the engine, and once in the player screen, which showed "looping" whenever no source was
 * attached yet. Turning the loop off before the first play made the two disagree.
 *
 * Per-item defaults live on [PlaybackPolicy] and apply from the moment an item is loaded: a sleep
 * sound repeats until the timer stops it, a story that repeats has not ended. This is what the
 * session publishes until an item makes the question concrete.
 */
const val DEFAULT_LOOPING = true

/**
 * Why the session could not play an item — and *which* item.
 *
 * The id is part of the failure because a lookup that fails releases the session's claim on the
 * item it was for: by the time the failure is published, the session has fallen back to whatever
 * was playing before, or to nothing. A screen asking "is this failure mine?" has to compare against
 * the failure's own item. Gating on the session's current one hid every resolution error.
 */
sealed interface PlaybackFailure {
    val itemId: PlaybackItemId

    /**
     * Nothing could find this item: the catalog does not hold it, or — for a kind the session has
     * no resolver for yet — nothing is able to look. Tapping again cannot help.
     */
    data class ItemNotFound(override val itemId: PlaybackItemId) : PlaybackFailure

    /** The item exists, but no source could be produced for it. Worth another tap. */
    data class SourceUnavailable(override val itemId: PlaybackItemId) : PlaybackFailure

    /** The engine accepted a source and then failed on it. */
    data class EngineFailed(override val itemId: PlaybackItemId) : PlaybackFailure
}

/**
 * The playback view the application layer works with.
 *
 * Deliberately engine-independent: features consume this shared projection instead of the playback
 * engine's technical state model. Deliberately content-independent too — a [PlaybackItemId] names
 * a sound or a story, and the session is the one place that has to hold either.
 *
 * Two item ids, because during a switch they genuinely differ: the listener has asked for B while A
 * is still the sound coming out of the speaker. Collapsing them into one field published "B is
 * playing" for as long as B took to resolve.
 */
data class PlaybackSummary(
    /**
     * The item the listener last asked for.
     *
     * What a screen highlights and what its controls act on — set from the moment of the tap, before
     * any source lookup. `null` when the session has never been given an item.
     */
    val requestedItemId: PlaybackItemId? = null,
    /**
     * The item the engine is actually holding, and therefore the one [isPlaying] describes.
     *
     * Differs from [requestedItemId] while a switch is in flight, and goes `null` while a dropped
     * service connection is being restored — the session's own choice lives on in [requestedItemId].
     */
    val activeItemId: PlaybackItemId? = null,
    /**
     * Whether playback is *wanted*.
     *
     * This — not [isPlaying] — is what a play/pause control renders, and what a screen branches on
     * when the listener taps it. The two used to differ: the session decided from the desired state
     * while the screen drew the actual one, so during buffering a listener saw a Play icon and got
     * a pause out of tapping it.
     */
    val playIntent: Boolean = false,
    /** Whether the engine is producing sound right now — for [activeItemId], not for the request. */
    val isPlaying: Boolean = false,
    /** The requested item is wanted but not audible yet: being resolved, loaded or buffered. */
    val isPreparing: Boolean = false,
    val isLooping: Boolean = DEFAULT_LOOPING,
    val volume: Float = 1.0f,
    val failure: PlaybackFailure? = null,
)

/**
 * The raw value of [PlaybackSummary.requestedItemId] when it is of [kind], and `null` when the
 * session is on something else entirely.
 *
 * A list of sounds has nothing to say about a story being played. Asking "is this row the current
 * item?" without checking the kind would light up the sound whose id a story happens to share.
 */
fun PlaybackSummary.requestedValueOf(kind: PlaybackKind): String? =
    requestedItemId?.takeIf { it.kind == kind }?.value
