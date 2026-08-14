package com.xwab.app.core.catalog

// Explicit because this is common code: `kotlin.jvm.*` is a default import on the JVM only, so
// Android compiles this file without the import and the Kotlin/Native targets do not.
import kotlin.jvm.JvmInline

/**
 * The identity of one catalog category.
 *
 * The counterpart to [TrackId], for the same reason and against the same mistake. [TrackId]'s own
 * note names it: `play(categoryId)` used to compile. So did `observeCategory(trackId.value)`, and
 * so did handing a track's name to a lookup that wanted a category — every one of them a runtime
 * miss that reads like missing content rather than a wrong argument.
 *
 * `@JvmInline` erases to the [String] it holds, so the safety costs nothing at runtime while it
 * stays in a typed position.
 *
 * Deliberately absent from `CategoryRoute`, which keeps a plain [String]. A route is a serialized
 * wire format that Navigation 3 persists, and the seam where it becomes a [CategoryId] is the
 * feature's entry — exactly where [TrackId] is rebuilt from `PlayerRoute`.
 */
@JvmInline
value class CategoryId(val value: String) {
    init {
        // The same check [TrackId] makes: an id that is blank names a category no lookup can
        // match, so it is refused where it is built rather than where it fails.
        require(value.isNotBlank()) { "A category id cannot be blank." }
    }

    override fun toString(): String = value
}
