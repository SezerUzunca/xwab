package com.xwab.app.core.session.port

/**
 * What looping should be before anything has been loaded and before the listener has chosen.
 *
 * The one place this default lives. It used to be stated twice — once in the session, deciding what
 * to hand the engine, and once in the player screen, which showed "looping" whenever no source was
 * attached yet. Turning the loop off before the first play made the two disagree.
 *
 * Once an item is loaded, the session applies that content kind's own default: a sleep sound
 * repeats until the timer stops it, while a story does not repeat. This constant controls only
 * what the session publishes until an item makes the question concrete.
 */
const val DEFAULT_LOOPING: Boolean = true

/**
 * The volume a session accepts and reports.
 *
 * Stated on the port because it is part of the contract, not a property of one adapter: callers
 * pass a fraction of full volume, and a screen rendering [PlaybackSummary.volume] can trust the
 * bound rather than defend against it.
 */
val VOLUME_RANGE: ClosedFloatingPointRange<Float> = 0.0f..1.0f

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

    /**
     * The item exists, but nothing playable could be got for it: either no source was produced, or
     * the engine was handed one and never managed to open it. Both are reach failures — the
     * ordinary one being a listener with no network — and both are worth another tap.
     */
    data class SourceUnavailable(override val itemId: PlaybackItemId) : PlaybackFailure

    /** The engine accepted a source and then failed on it. Tapping again is unlikely to help. */
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
    /**
     * What [requestedItemId] calls itself, or `null` while the session does not yet know.
     *
     * Published because the app shell draws a now-playing bar on every screen, and the shell has no
     * other honest way to name what is playing: a [PlaybackItemId] is a kind and a raw string, so
     * turning one into a title outside this module would mean a `when` over content kinds in the
     * shell and a dependency on both catalogs — which is the coupling this summary exists to
     * remove. The session already resolves the title on its way to the engine; it just used to
     * throw it away.
     *
     * This is the name the app's own lists use, which is not always the one the platform is given:
     * a sound listed as "Rain on the Window" is announced in the notification as "Gentle Rain", and
     * one lullaby has it the other way round. A screen must show what the screen before it showed.
     * After a reconnect to a service that outlived the process the session no longer holds that
     * name, and the platform's is published instead — a notification's name beats no name.
     *
     * Never a title belonging to some *other* item. During a switch the engine still holds the
     * outgoing item while [requestedItemId] names the incoming one, so the title is published only
     * while the two agree; until then it is null and a screen shows [isPreparing] instead. A bar
     * that named the previous sound while preparing the next one would be worse than a bar with no
     * name at all.
     */
    val title: String? = null,
    val isLooping: Boolean = DEFAULT_LOOPING,
    /**
     * How loud the session is, always within [VOLUME_RANGE].
     *
     * Guaranteed here rather than left to whoever renders it. This is the projection features read
     * instead of the engine's own state model, and a raw engine float with no stated range is
     * exactly the technical detail that projection exists to keep out: a slider reading it had to
     * clamp defensively, and a second screen reading it would have had to remember to.
     */
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
