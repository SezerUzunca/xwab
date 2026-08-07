package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.StoryId

/**
 * Where one story's audio is played from. Always remote, never cached.
 *
 * Checked here as well as on the row it is built from, because this is the address that leaves the
 * app: a story is handed to the platform player directly, so it never passes through `core:network`
 * and never meets the HTTPS check made there.
 */
data class StoryStreamSource(val httpsUrl: String) {
    init {
        require(httpsUrl.startsWith("https://")) { "Story audio must stream over HTTPS." }
    }
}

/**
 * The manifest's second port: the source behind each story, read by `core:playback:session` and by
 * nothing else.
 *
 * This is the story-side twin of `AudioSourceCatalog` in `core:sound:manifest`, and it is kept apart
 * from `StoryCatalogRepository` for the same reason: a screen depends on `core:story:catalog` for
 * the repository, and if this port lived there too a screen could resolve it out of the container
 * and read the URL. It lives here, no feature declares this module, and `checkArchitecture` rule 4
 * fails the build on one that tries.
 *
 * There is no cache side to this port. A sound's source names a file to keep; a story's names only
 * an address to stream from.
 */
interface StoryStreamCatalog {
    /**
     * The source for [storyId], or null when the manifest holds no such story.
     */
    fun sourceFor(storyId: StoryId): StoryStreamSource?
}

internal class ManifestStoryStreamCatalog(
    entries: List<StoryEntry> = storyManifest,
) : StoryStreamCatalog {
    private val sourcesById: Map<StoryId, StoryStreamSource> = entries.associate { entry ->
        entry.story.id to StoryStreamSource(entry.httpsUrl)
    }

    override fun sourceFor(storyId: StoryId): StoryStreamSource? = sourcesById[storyId]
}
