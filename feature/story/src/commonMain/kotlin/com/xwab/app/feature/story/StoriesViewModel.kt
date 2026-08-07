package com.xwab.app.feature.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackFailure
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackKind
import com.xwab.app.core.playbacksession.requestedValueOf
import com.xwab.app.core.story.StoryId
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class StoriesViewModel(
    observeStoriesContentUseCase: ObserveStoriesContentUseCase,
    private val playbackCoordinator: PlaybackCoordinator,
) : ViewModel() {
    val state: StateFlow<StoriesState> = observeStoriesContentUseCase()
        .map { content ->
            val playback = content.playback
            // This screen lists stories, so the session being on a sound is the same to it as the
            // session being on nothing: no row here is the current item.
            val requestedStoryId = playback.requestedValueOf(PlaybackKind.STORY)?.let(::StoryId)
            // Bound locally: `failure` is another module's property, so the checks below cannot
            // smart-cast it in place.
            val failure = playback.failure?.takeIf { it.itemId.kind == PlaybackKind.STORY }

            StoriesState(
                stories = content.stories,
                requestedStoryId = requestedStoryId,
                playIntent = requestedStoryId != null && playback.playIntent,
                isPreparing = requestedStoryId != null && playback.isPreparing,
                error = failure?.asStoryError(),
                failedStoryId = failure?.let { StoryId(it.itemId.value) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StoriesState(),
        )

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(storyId: StoryId) {
        val current = state.value
        if (current.requestedStoryId == storyId && current.playIntent) {
            playbackCoordinator.pause()
        } else {
            viewModelScope.launch { playbackCoordinator.play(PlaybackItemId.story(storyId.value)) }
        }
    }
}

/**
 * A story the catalog has dropped and one that could not be reached read the same on screen
 * otherwise, and they are not the same advice: one is a dead end, the other is worth another tap.
 */
private fun PlaybackFailure.asStoryError(): StoryError = when (this) {
    is PlaybackFailure.ItemNotFound -> StoryError.NotFound
    is PlaybackFailure.SourceUnavailable -> StoryError.Unavailable
    is PlaybackFailure.EngineFailed -> StoryError.CouldNotOpen
}
