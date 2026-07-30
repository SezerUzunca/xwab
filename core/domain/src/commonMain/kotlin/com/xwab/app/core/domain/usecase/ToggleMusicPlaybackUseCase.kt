package com.xwab.app.core.domain.usecase

import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import kotlinx.coroutines.flow.first

/**
 * Starts or stops playback of a track the caller knows only by id.
 *
 * The one use case more than one screen performs, which is why it lives here: home, category and
 * player all toggle playback, and all three would otherwise repeat the catalog lookup and the
 * decision to ignore an unknown id. The lookup cannot move down into `core:playback` either —
 * [PlaybackCoordinator] takes a resolved track on purpose, so the playback session never depends
 * on the catalog.
 *
 * A use case only one screen performs belongs to that screen's feature module instead; keeping it
 * here would make every new screen a change to `core:domain` and a rebuild of every feature, and
 * `checkArchitecture` fails the build when one leaks back in.
 */
class ToggleMusicPlaybackUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    suspend operator fun invoke(musicId: String) {
        val music = musicCatalog.observeMusic(musicId).first() ?: return
        playbackCoordinator.togglePlayback(music)
    }
}
