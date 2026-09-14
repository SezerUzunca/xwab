package com.xwab.app.core.sources.port

/**
 * The two namespaces this module publishes sources under.
 *
 * Declared here, not re-typed at each call site: `:core:session` reads sources under these names
 * and `:core:delivery` caches under the same names through `CacheKey`, so one changed literal
 * would otherwise leave the other silently unable to find what it is looking for.
 */
const val SOUND_NAMESPACE: String = "sound"
const val STORY_NAMESPACE: String = "story"

/** Read access to physical content addresses, kept out of every screen-facing content port. */
interface SourcePort {
    /** The source for [itemId] in [namespace], or null when no such source is published. */
    fun sourceFor(namespace: String, itemId: String): ContentSource?

    /** Every cache filename still referenced by [namespace]. Streaming-only sources are omitted. */
    fun cacheFileNames(namespace: String): Set<String>
}
