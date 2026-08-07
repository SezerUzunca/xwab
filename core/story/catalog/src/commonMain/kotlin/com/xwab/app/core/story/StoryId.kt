package com.xwab.app.core.story

// Explicit because this is common code: `kotlin.jvm.*` is a default import on the JVM only, so
// Android compiles this file without the import and the Kotlin/Native targets do not.
import kotlin.jvm.JvmInline

/**
 * The identity of one sleep story.
 *
 * A type of its own rather than a bare `String`, for the reason the sound catalog wraps its track
 * ids too: these ids travel through navigation routes, favorites-like sets and screen state, all of
 * which handle other strings too. Passing a category id where a story is meant must not compile.
 *
 * Playback is the one place the raw value is handed over deliberately. The session pairs it with a
 * kind, which is what keeps a story and a sound of the same name two different items there.
 *
 * `@JvmInline` erases to the `String` it holds, so the safety costs nothing at runtime while the id
 * stays in a typed position. Unwrap it only at the edges where a string genuinely is the format: a
 * serialized route, a Compose lazy-list key, a namespaced id handed to the playback engine.
 *
 * It refuses a blank value, exactly as the sound catalog's track id does. An empty id is a story
 * that can be listed and never opened, so a typo is rejected where the manifest is built.
 */
@JvmInline
value class StoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "A story id cannot be blank." }
    }

    override fun toString(): String = value
}
