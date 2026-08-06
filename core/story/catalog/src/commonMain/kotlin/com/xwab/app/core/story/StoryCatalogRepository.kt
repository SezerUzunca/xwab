package com.xwab.app.core.story

import kotlinx.coroutines.flow.Flow

/**
 * Read access to the story catalog.
 *
 * This is the whole of what a screen may know about stories. The list itself and the source behind
 * each story's audio live in `core:story:manifest`, which no feature declares — so nothing
 * reachable from here leads to a URL.
 *
 * A `Flow` rather than a `suspend fun` because the list is not permanent the way the shipped sound
 * manifest is: the placeholder list is served once today, and a feed-backed implementation will
 * emit again when it refreshes, without the port changing shape.
 */
interface StoryCatalogRepository {
    fun observeStories(): Flow<List<Story>>

    /** The story with [storyId], or `null` when the catalog holds no such story. */
    fun observeStory(storyId: StoryId): Flow<Story?>
}
