package com.xwab.app.core.story

// Explicit because this is common code: `kotlin.jvm.*` is a default import on the JVM only, so
// Android compiles this file without the import and the Kotlin/Native targets do not.
import kotlin.jvm.JvmInline

/**
 * The identity of one sleep story.
 *
 * A type of its own rather than a bare `String`, for the reason `TrackId` is one in `core:sound:catalog`:
 * these ids will travel through navigation routes, playback requests and — once stories are played
 * — a session that also carries sound ids. `play(trackId)` must not compile where a story is meant,
 * and neither must the reverse.
 *
 * `@JvmInline` erases to the `String` it holds, so the safety costs nothing at runtime while the id
 * stays in a typed position. Unwrap it only at the edges where a string genuinely is the format: a
 * serialized route, a Compose lazy-list key, a namespaced id handed to the playback engine.
 *
 * Unlike `TrackId` it refuses a blank value. A story id is going to arrive from a feed rather than
 * from a hand-written list one module away, and an empty id there would otherwise surface as a
 * story that can be listed but never opened.
 */
@JvmInline
value class StoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "A story id cannot be blank." }
    }

    override fun toString(): String = value
}
