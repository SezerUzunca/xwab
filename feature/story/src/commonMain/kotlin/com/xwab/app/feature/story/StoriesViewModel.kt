package com.xwab.app.feature.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.playback.port.PlaybackPort
import com.xwab.app.core.playback.port.PlaybackItemId
import com.xwab.app.core.playback.port.PlaybackKind
import com.xwab.app.core.playback.port.requestedValueOf
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.designsystem.state.Loadable
import com.xwab.app.feature.story.domain.ObserveStoriesContentUseCase
import com.xwab.app.feature.story.domain.StoriesContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class StoriesViewModel(
    observeStoriesContentUseCase: ObserveStoriesContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    val state: StateFlow<Loadable<StoriesState>> = observeStoriesContentUseCase()
        .map<StoriesContent, Loadable<StoriesState>> { content ->
            val playback = content.playback
            // This screen lists stories, so the session being on a sound is the same to it as the
            // session being on nothing: no row here is the current item.
            val requestedStoryId = playback.requestedValueOf(PlaybackKind.STORY)?.let(::StoryId)
            // Bound locally: `failure` is another module's property, so the checks below cannot
            // smart-cast it in place.
            val failure = playback.failure?.takeIf { it.itemId.kind == PlaybackKind.STORY }

            Loadable.Ready(StoriesState(
                stories = content.stories,
                requestedStoryId = requestedStoryId,
                playIntent = requestedStoryId != null && playback.playIntent,
                isPreparing = requestedStoryId != null && playback.isPreparing,
                playbackFailure = failure,
            ))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    fun togglePlayback(storyId: StoryId) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.requestedStoryId == storyId && current.playIntent) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(PlaybackItemId.story(storyId.value)) }
        }
    }
}
