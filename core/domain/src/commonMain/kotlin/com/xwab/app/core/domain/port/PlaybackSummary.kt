package com.xwab.app.core.domain.port

/**
 * The playback view the application layer works with.
 *
 * Deliberately domain-owned: use cases and features must not see the engine's own state model,
 * so the adapter behind [PlaybackCoordinator] maps into this and nothing else crosses over.
 */
data class PlaybackSummary(
    val activeSourceId: String? = null,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = false,
    val volume: Float = 1.0f,
    val hasFailed: Boolean = false,
)
