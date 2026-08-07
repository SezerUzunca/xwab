package com.xwab.app.feature.story

import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId

internal enum class StoryError {
    /** The catalog no longer holds the story a row was drawn from. Tapping again cannot help. */
    NotFound,

    /** No source could be produced. A story streams and is never cached, so this is the likely one. */
    Unavailable,

    /** The engine took a source and then failed on it. */
    CouldNotOpen,
}

internal data class StoriesState(
    val stories: List<Story> = emptyList(),
    /** The story the session was last asked for, or null when it is on a sound or on nothing. */
    val requestedStoryId: StoryId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested story is wanted but not audible yet. */
    val isPreparing: Boolean = false,
    /**
     * A failure and the row it belongs to, kept together.
     *
     * A failed lookup releases the session's claim, so by the time this arrives the session has
     * already fallen back to whatever came before — [requestedStoryId] is no longer the story that
     * failed. Drawing the message under the right row needs the failure's own id.
     */
    val error: StoryError? = null,
    val failedStoryId: StoryId? = null,
)
