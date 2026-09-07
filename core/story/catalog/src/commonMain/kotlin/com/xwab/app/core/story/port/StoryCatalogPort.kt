package com.xwab.app.core.story.port

import kotlinx.coroutines.flow.Flow

/**
 * Read access to the story catalog.
 *
 * This is the whole of what a screen may know about stories. The list itself and the source behind
 * each story's audio live in `core:story:manifest`, which no feature declares — so nothing
 * reachable from here leads to a URL.
 *
 * A `Flow` keeps this read port consistent with the sound catalog and lets consumers observe it
 * without knowing how the manifest stores its rows.
 */
public interface StoryCatalogPort {
    public fun observeStories(): Flow<List<Story>>

    /** The story with [storyId], or `null` when the catalog holds no such story. */
    public fun observeStory(storyId: StoryId): Flow<Story?>
}
