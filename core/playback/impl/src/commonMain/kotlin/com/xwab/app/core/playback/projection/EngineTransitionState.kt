package com.xwab.app.core.playback.projection

/** What the engine reads off its native player to decide whether to keep watching it. */
internal data class EngineTransitionState(
    val hasCurrentItem: Boolean,
    val hasFailure: Boolean,
    val isReadyToPlay: Boolean,
    val isWaitingToPlay: Boolean,
    val playTransitionTicksRemaining: Int,
    val awaitingReadiness: Boolean,
)
