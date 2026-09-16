package com.xwab.app.feature.story

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId

/** Loading and content states owned by this feature. */
internal sealed interface StoriesUiState {
    data object Loading : StoriesUiState

    data class Ready(val value: StoriesState) : StoriesUiState
}

/** Content available in [StoriesUiState.Ready]. */
internal data class StoriesState(
    val stories: List<Story> = emptyList(),
    /** The story the session was last asked for, or null when it is on a sound or on nothing. */
    val requestedStoryId: StoryId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested story is wanted but not audible yet. */
    val isPreparing: Boolean = false,
    /**
     * The session's failure, carried as the session reports it.
     *
     * This used to be a local enum mirroring [PlaybackFailure] one-for-one, plus a separate
     * `failedStoryId` — both redundant, because a failure already names the item it happened to.
     * A failed lookup releases the session's claim, so by the time this arrives
     * [requestedStoryId] is no longer the story that failed; the row is found through
     * [rowFailure] instead.
     */
    val playbackFailure: PlaybackFailure? = null,
) {
    /**
     * What one row shows. Answered here rather than at the call site that draws it, so that the
     * question and the fields it reads stay in one file.
     *
     * The three used to be spelled out inside the composable, and [rowFailure] in particular
     * compared raw id strings — safe only because the ViewModel had already dropped failures of
     * another kind, a guarantee that lived in a different file. It matches the whole item id now,
     * so it holds on its own.
     */
    fun isRowPlaying(storyId: StoryId): Boolean = requestedStoryId == storyId && playIntent

    fun isRowPreparing(storyId: StoryId): Boolean = requestedStoryId == storyId && isPreparing

    fun rowFailure(storyId: StoryId): PlaybackFailure? =
        playbackFailure?.takeIf { it.itemId == PlaybackItemId.story(storyId.value) }
}
