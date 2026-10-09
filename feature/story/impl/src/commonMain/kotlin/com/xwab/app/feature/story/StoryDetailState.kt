package com.xwab.app.feature.story

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.story.port.Story

internal sealed interface StoryDetailUiState {
    data object Loading : StoryDetailUiState
    data class Ready(val value: StoryDetailState) : StoryDetailUiState
}

internal data class StoryDetailState(
    val story: Story? = null,
    val playIntent: Boolean = false,
    val isPreparing: Boolean = false,
    val failure: PlaybackFailure? = null,
) {
    /** A removed item can still be paused if it is already playing. */
    val canPlay: Boolean get() = story != null || playIntent
}
