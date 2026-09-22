package com.xwab.app.core.session.port

/**
 * How one kind of content becomes something the playback engine can open.
 *
 * The session owns the contract it needs; each content module owns its implementation and
 * contributes it under its stable playback kind. The session selects a resolver from the injected
 * map without depending on any content module. Adding or removing a kind changes its contribution
 * and feature wiring, not the session implementation.
 *
 * This is a core adapter contract. Screens use [PlaybackPort]. The module's architecture policy
 * marks this interface and its result models as adapter-only; `checkArchitecture` rejects feature
 * references even though these public types share the session module's compilation classpath.
 */
fun interface PlaybackItemResolver {
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
     *   listed in English, announced as "Egawa Nwa". Both are deliberate, and the session has to
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
