package com.xwab.app.core.playbacksession

/**
 * How the session turns one kind of item into something the engine can open.
 *
 * `internal` on purpose, and the reason this seam is inside the session rather than in modules of
 * its own: a resolver hands back a URI, so a public one would let any screen resolve
 * `PlaybackItemResolver` out of the container and read the address of a file the session is
 * supposed to be the only route to. Kept here, the only thing a screen can reach is
 * [PlaybackCoordinator].
 *
 * Adding a kind is adding one implementation and one line in the DI module. Nothing else in the
 * build changes shape — which is the whole point of the session speaking [PlaybackItemId].
 */
internal interface PlaybackItemResolver {
    val kind: PlaybackKind

    /** @param value the raw half of a [PlaybackItemId] whose kind is [kind]. */
    suspend fun resolve(value: String): ItemResolution
}

/**
 * What playing this item means, beyond where its bytes are.
 *
 * @param defaultLooping what looping should be when the listener has not said. A sleep sound
 *   repeats until the timer stops it; a story that repeats has not ended, it has restarted. An
 *   explicit choice still wins — this is the default, not the policy.
 */
internal data class PlaybackPolicy(val defaultLooping: Boolean)

internal sealed interface ItemResolution {
    /**
     * @param title what the platform media session should publish, read beside the URI rather than
     *   handed in by a screen, so a stale title cannot be paired with a fresh source.
     */
    data class Resolved(
        val uri: String,
        val title: String?,
        val artist: String?,
        val policy: PlaybackPolicy,
    ) : ItemResolution

    /** The catalog does not hold this item at all. */
    data object NotFound : ItemResolution

    /** The item exists; its audio could not be reached. */
    data class Unavailable(val reason: String?) : ItemResolution
}
