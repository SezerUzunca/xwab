package com.xwab.app.core.catalog

/**
 * The identity of one catalog track.
 *
 * A wrapper rather than a bare `String` because these ids travel a long way — a navigation route, a
 * favorites set, a playback request, the name a download caches under — and every stop on that
 * journey handles other strings too. `play(categoryId)` used to compile, and so did asking the
 * favorites set whether it contained a category.
 *
 * `@JvmInline` erases to the `String` it holds, so the safety costs nothing at runtime. The two
 * places it is deliberately unwrapped are the edges where a string is the format: a serialized
 * navigation route, and `core:playback-engine`, which is a standalone library that knows nothing
 * about this app's catalog.
 */
@JvmInline
value class TrackId(val value: String) {
    override fun toString(): String = value
}
