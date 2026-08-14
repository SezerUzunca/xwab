package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.Story

/**
 * One story, and where its audio streams from.
 *
 * The mirror of `CatalogEntry` on the sound side, with one field missing on purpose: there is no
 * cache file name, because a story is never written to disk. Every published row is playable.
 */
internal class StoryEntry(
    val story: Story,
    val httpsUrl: String,
) {
    init {
        require(httpsUrl.startsWith("https://")) {
            "Story audio must use HTTPS: ${story.id.value}"
        }
    }
}
