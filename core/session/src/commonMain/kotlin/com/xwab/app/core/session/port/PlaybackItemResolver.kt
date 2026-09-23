package com.xwab.app.core.session.port

/**
 * Marks the session's resolver contract, which content capabilities implement and screens never
 * touch.
 *
 * These types have to be public in Kotlin: a content module implements them across a module
 * boundary, and Kotlin has no visibility for "these modules and no others" yet (KEEP-0451 proposes
 * one). Until it does, the compiler enforces the boundary this way. Using any of them is an error
 * unless the using code opts in, which a resolver and this module's own adapter do and a feature
 * has no reason to. `checkArchitecture` still rejects a feature that references them — the opt-in
 * included, since this annotation is adapter-only too — so the two checks cover each other.
 */
@RequiresOptIn(
    message = "The playback resolver contract is for content capabilities that contribute a " +
        "resolver. Screens steer playback through PlaybackPort.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class PlaybackResolverApi

/**
 * How one kind of content becomes something the playback engine can open.
 *
 * The session owns the contract it needs; each content module owns its implementation and
 * contributes it under its stable playback kind. The session selects a resolver from the injected
 * map without depending on any content module. Adding or removing a kind changes its contribution
 * and feature wiring, not the session implementation.
 *
 * This is a core adapter contract. Screens use [PlaybackPort]. Using it requires opting in to
 * [PlaybackResolverApi], and the module's architecture policy marks it and its result models as
 * adapter-only, so `checkArchitecture` rejects feature references as well.
 */
@PlaybackResolverApi
fun interface PlaybackItemResolver {
    /**
     * @param value the raw half of a playback item id — what the kind's own catalog calls the item.
     *   The kind itself is not passed: a resolver is registered under exactly one, so it already
     *   knows.
     */
    suspend fun resolve(value: String): ItemResolution
}

/**
 * What playing this item means, beyond where its bytes are.
 *
 * @param defaultLooping what looping should be when the listener has not said. A sleep sound
 *   repeats until the timer stops it; a story that repeats has not ended, it has restarted. An
 *   explicit choice still wins — this is the default, not the policy.
 */
@PlaybackResolverApi
data class PlaybackPolicy(val defaultLooping: Boolean)

@PlaybackResolverApi
sealed interface ItemResolution {
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
    data class Resolved(
        val uri: String,
        val title: String?,
        val displayName: String,
        val artist: String?,
        val policy: PlaybackPolicy,
    ) : ItemResolution

    /** The catalog does not hold this item at all. */
    data object NotFound : ItemResolution

    /** The item exists; its audio could not be reached. */
    data class Unavailable(val reason: String?) : ItemResolution
}
