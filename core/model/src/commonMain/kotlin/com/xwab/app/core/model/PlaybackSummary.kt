package com.xwab.app.core.model

/**
 * The playback view the application layer works with.
 *
 * Deliberately engine-independent: features consume this shared projection instead of the
 * playback engine's technical state model.
 */
data class PlaybackSummary(
    val activeSourceId: String? = null,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = false,
    val volume: Float = 1.0f,
    val hasFailed: Boolean = false,
)
