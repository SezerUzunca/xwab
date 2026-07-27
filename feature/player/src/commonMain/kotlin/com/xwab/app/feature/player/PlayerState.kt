package com.xwab.app.feature.player

import com.xwab.app.core.model.Music

internal enum class PlayerError {
    AudioNotFound,
    AudioCouldNotOpen,
}

internal data class PlayerState(
    val music: Music? = null,
    val isFavorite: Boolean = false,
    val isPlaying: Boolean = false,
    val isLooping: Boolean = true,
    val volume: Float = 1.0f,
    val sleepTimerRemainingMs: Long? = null,
    val error: PlayerError? = null,
)
