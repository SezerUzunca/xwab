package com.xwab.app.feature.player.domain

import com.xwab.app.core.playback.PlaybackCoordinator

/**
 * Looping, volume and the sleep timer are steered from the player screen and nowhere else, so
 * these commands live with that screen. They stay use cases rather than direct coordinator calls
 * from the ViewModel: the port is the domain's, and the screen keeps talking to it in one voice.
 */

internal class SetPlaybackLoopingUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(enabled: Boolean) {
        playbackCoordinator.setLooping(enabled)
    }
}

internal class SetPlaybackVolumeUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(volume: Float) {
        playbackCoordinator.setVolume(volume)
    }
}

internal class StartSleepTimerUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(durationMs: Long) {
        playbackCoordinator.startSleepTimer(durationMs)
    }
}

internal class CancelSleepTimerUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke() {
        playbackCoordinator.cancelSleepTimer()
    }
}
