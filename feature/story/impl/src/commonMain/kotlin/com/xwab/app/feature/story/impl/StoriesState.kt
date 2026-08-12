package com.xwab.app.feature.story.impl

import com.xwab.app.core.playbacksession.PlaybackFailure
import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId

/** Content available after the outer [com.xwab.app.core.ui.state.Loadable] becomes ready. */
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
     * `playbackFailure.itemId` instead.
     */
    val playbackFailure: PlaybackFailure? = null,
)
