package com.xwab.app.feature.favorites.impl.domain

import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class FavoritesContent(
    val musics: List<Music>,
    val playback: PlaybackSummary,
)

/** Joins only the ports required by the user's saved-sounds capability. */
internal class ObserveFavoritesContentUseCase(
    private val musicCatalog: MusicCatalogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
) {
    operator fun invoke(): Flow<FavoritesContent> = combine(
        musicCatalog.observeAllMusic(),
        favoritesRepository.favoriteIds,
        playbackCoordinator.playback,
    ) { musics, favoriteIds, playback ->
        FavoritesContent(
            musics = musics.filter { it.id in favoriteIds },
            playback = playback,
        )
    }
}
