package com.xwab.app.feature.favorites.domain

import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class FavoritesContent(
    val musics: List<Music>,
    val playback: PlaybackSummary,
)

/** Joins only the ports required by the user's saved-sounds capability. */
internal class ObserveFavoritesContentUseCase(
    private val soundPort: SoundPort,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(): Flow<FavoritesContent> = combine(
        soundPort.observeAllMusic(),
        favoritesPort.observe("music").map { ids -> ids.mapTo(mutableSetOf(), ::TrackId) },
        playbackPort.playback,
    ) { musics, favoriteIds, playback ->
        FavoritesContent(
            musics = musics.filter { it.id in favoriteIds },
            playback = playback,
        )
    }
}
