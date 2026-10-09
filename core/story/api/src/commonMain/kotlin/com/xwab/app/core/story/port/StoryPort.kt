package com.xwab.app.core.story.port

import kotlinx.coroutines.flow.Flow

/** Read access to story metadata. Physical stream addresses remain private to this module. */
interface StoryPort {
    fun observeStories(): Flow<List<Story>>
    fun observeStory(storyId: StoryId): Flow<Story?>
}
