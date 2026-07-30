package com.xwab.app.core.domain.usecase

import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import kotlinx.coroutines.flow.first

/**
 * The use cases more than one feature performs.
 *
 * A use case only a single screen performs belongs to that screen's feature module instead —
 * keeping it here would make every new screen a change to `core:domain` and a rebuild of every
 * feature. `checkArchitecture` fails the build when one leaks back in.
 */

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
        val music = musicCatalog.observeMusic(musicId).first() ?: return
        playbackCoordinator.togglePlayback(music)
    }
}
