package com.xwab.app.core.resolution.port

/**
 * How one kind of content becomes something the playback engine can open.
 *
 * This is the seam that makes a content type pluggable. A content module implements it, contributes
 * it into the application scope keyed by its own playback kind, and `:core:session` — which
 * implements none of them and names none of them — resolves whatever it was handed by looking the
 * kind up in the injected map. Adding a fourth content type is adding a module; removing one is
 * deleting a directory. Neither edits the session.
 *
 * It lives in a module of its own rather than in `:core:session`, and that module is off limits to
 * features, because [ItemResolution.Resolved] carries the address of a file. Published from the
 * session, any screen could resolve a resolver out of the graph and read that address directly,
 * which is exactly the route `PlaybackPort` exists to be the only one of. Kept here, a feature
 * cannot name these types at all — the same compile-time guarantee the resolvers had while they
 * were internal to the session.
 */
public fun interface PlaybackItemResolver {
    /**
     * @param value the raw half of a playback item id — what the kind's own catalog calls the item.
     *   The kind itself is not passed: a resolver is registered under exactly one, so it already
     *   knows.
     */
    public suspend fun resolve(value: String): ItemResolution
}

/**
 * What playing this item means, beyond where its bytes are.
 *
 * @param defaultLooping what looping should be when the listener has not said. A sleep sound
 *   repeats until the timer stops it; a story that repeats has not ended, it has restarted. An
 *   explicit choice still wins — this is the default, not the policy.
 */
public data class PlaybackPolicy(public val defaultLooping: Boolean)

public sealed interface ItemResolution {
    /**
     * @param title what the platform media session should publish, read beside the source rather
     *   than handed in by a screen, so a stale title cannot be paired with a fresh source.
     * @param displayName what this app calls the item on its own screens, which is not always
     *   [title]. A sound's catalog name is the recording's — "Rain on the Window" — while the
     *   notification gets the plainer "Gentle Rain", and one lullaby has it the other way round:
     *   listed in English, announced as "Egwu Nwa". Both are deliberate, and the session has to
     *   carry both, because a now-playing bar two rows below the list that named it cannot call it
     *   something else.
     */
    public data class Resolved(
        public val uri: String,
        public val title: String?,
        public val displayName: String,
        public val artist: String?,
        public val policy: PlaybackPolicy,
    ) : ItemResolution

    /** The catalog does not hold this item at all. */
    public data object NotFound : ItemResolution

    /** The item exists; its audio could not be reached. */
    public data class Unavailable(public val reason: String?) : ItemResolution
}
