package com.xwab.app.feature.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.domain.ObserveStoryContentUseCase
import com.xwab.app.feature.story.domain.StoryContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class StoryDetailViewModel(
    storyId: StoryId,
    observeStoryContentUseCase: ObserveStoryContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    private val itemId = PlaybackItemId(STORY_PLAYBACK_KIND, storyId.value)

    val state: StateFlow<StoryDetailUiState> = observeStoryContentUseCase(storyId)
        .map<StoryContent, StoryDetailUiState> { content ->
            val playback = content.playback
            val isRequested = playback.requestedItemId == itemId
            StoryDetailUiState.Ready(StoryDetailState(
                story = content.story,
                playIntent = isRequested && playback.playIntent,
                isPreparing = isRequested && playback.isPreparing,
                failure = playback.failure?.takeIf { it.itemId == itemId },
            ))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryDetailUiState.Loading)

    /** The session's timer for the shortcut; kept out of [state] because it ticks every second. */
    val sleepTimerRemainingMs: StateFlow<Long?> =
        playbackPort.sleepTimerRemainingMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePlayback() {
        val current = (state.value as? StoryDetailUiState.Ready)?.value ?: return
        if (!current.canPlay) return
        if (current.playIntent) playbackPort.pause()
        else viewModelScope.launch { playbackPort.play(itemId) }
    }
}
