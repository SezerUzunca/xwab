package com.xwab.app.core.catalog

// Explicit because this is common code: `kotlin.jvm.*` is a default import on the JVM only, so
// Android compiles this file without the import and the Kotlin/Native targets do not.
import kotlin.jvm.JvmInline

/**
 * The identity of one catalog track.
 *
 * A wrapper rather than a bare `String` because these ids travel a long way — a navigation route, a
 * favorites set, a playback request, the name a download caches under — and every stop on that
 * journey handles other strings too. `play(categoryId)` used to compile, and so did asking the
 * favorites set whether it contained a category.
 *
 * `@JvmInline` erases to the `String` it holds, so the safety costs nothing at runtime — but only
 * while it stays in a typed position. Anywhere it would be passed as `Any` it boxes back into an
 * object, which is why lazy-list keys take `id.value`: Compose stores those in a `Bundle`, and a
 * boxed value class is not something a `Bundle` can hold.
 *
 * The places it is deliberately unwrapped are all edges where a string is the format: the cache
 * file name a track downloads under, a serialized navigation route, a Compose list key, and
 * `core:playback:engine`, which is a standalone library that knows nothing about this app's catalog.
 */
@JvmInline
value class TrackId(val value: String) {
    init {
        // The same check the story catalog's id makes, for the same reason: an id that is blank
        // names a track no lookup can ever match, so it is refused where it is built rather than
        // where it fails.
        require(value.isNotBlank()) { "A track id cannot be blank." }
    }

    override fun toString(): String = value
}
