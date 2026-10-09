package com.xwab.app.feature.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.story.domain.ObserveStoryContentUseCase
import com.xwab.app.feature.story.domain.StoryContent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedInject
internal class StoryDetailViewModel(
    @Assisted storyId: StoryId,
    observeStoryContentUseCase: ObserveStoryContentUseCase,
    private val playbackPort: PlaybackPort,
) : ViewModel() {
    // Internal like the ViewModel it creates. Metro binds an assisted factory as itself rather
    // than through a generated provider, and an internal one still reaches the app graph.
    @Suppress("NON_PUBLIC_CONTRIBUTION_WARNING")
    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(storyId: StoryId): StoryDetailViewModel
    }

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

    /** The session's timer; kept out of [state] because it ticks every second. */
    val sleepTimerRemainingMs: StateFlow<Long?> =
        playbackPort.sleepTimerRemainingMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePlayback() {
        val current = (state.value as? StoryDetailUiState.Ready)?.value ?: return
        if (!current.canPlay) return
        if (current.playIntent) playbackPort.pause()
        else viewModelScope.launch { playbackPort.play(itemId) }
    }

    /**
     * Starts the session's timer and, when this story is not already playing, plays it. Restarting
     * it while the story plays leaves playback alone; a story the catalog no longer holds is not
     * started.
     */
    fun startSleepTimer(durationMs: Long) {
        if (durationMs <= 0L) return
        playbackPort.startSleepTimer(durationMs)
        val current = (state.value as? StoryDetailUiState.Ready)?.value ?: return
        if (!current.playIntent && current.canPlay) viewModelScope.launch { playbackPort.play(itemId) }
    }

    fun cancelSleepTimer() = playbackPort.cancelSleepTimer()
}
