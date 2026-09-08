package com.xwab.app.core.story.port

import kotlinx.coroutines.flow.Flow

/** Read access to story metadata and playable streams from the same catalog. */
public interface StoryPort {
    public fun observeStories(): Flow<List<Story>>
    public fun observeStory(storyId: StoryId): Flow<Story?>

    /** The stream for [storyId], or null when the catalog has no such story. */
    public fun sourceFor(storyId: StoryId): StoryStreamSource?
}
