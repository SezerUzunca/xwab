package com.xwab.app.core.domain.usecase

import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import kotlinx.coroutines.flow.first

class ToggleFavoriteUseCase(
    private val favoritesRepository: FavoritesRepository,
) {
    suspend operator fun invoke(musicId: String) {
        favoritesRepository.toggle(musicId)
    }
}

class ToggleMusicPlaybackUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    suspend operator fun invoke(musicId: String) {
        musicCatalog.observeMusic(musicId).first()?.let(playbackCoordinator::togglePlayback)
    }
}

class SetPlaybackLoopingUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(enabled: Boolean) {
        playbackCoordinator.setLooping(enabled)
    }
}

class SetPlaybackVolumeUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(volume: Float) {
        playbackCoordinator.setVolume(volume)
    }
}

class StartSleepTimerUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(durationMs: Long) {
        playbackCoordinator.startSleepTimer(durationMs)
    }
}

class CancelSleepTimerUseCase(
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke() {
        playbackCoordinator.cancelSleepTimer()
    }
}
