package com.xwab.app.core.domain.usecase

import com.xwab.app.core.media.AudioPlayerState
import com.xwab.app.core.media.PlaybackPhase

/**
 * The minimal playback view the application layer exposes to features.
 *
 * UI code should depend on this instead of the native [AudioPlayerState] /
 * [PlaybackPhase], so features stay unaware of the playback engine model.
 */
data class PlaybackSummary(
    val activeSourceId: String? = null,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = false,
    val volume: Float = 1.0f,
    val hasFailed: Boolean = false,
)

internal fun AudioPlayerState.toPlaybackSummary(): PlaybackSummary = PlaybackSummary(
    activeSourceId = activeSource?.id,
    isPlaying = isPlaying,
    isLooping = isLooping,
    volume = volume,
    hasFailed = phase == PlaybackPhase.Failed,
)
