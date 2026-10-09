package com.xwab.app.feature.story.navigation

import com.xwab.app.core.story.port.StoryId

/** Actions supplied by the application's composition root. */
class StoriesEntryCallbacks(
    val onStoryClick: (StoryId) -> Unit,
    val onBack: () -> Unit,
)
