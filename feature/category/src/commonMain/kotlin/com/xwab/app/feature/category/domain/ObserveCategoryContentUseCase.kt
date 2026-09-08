package com.xwab.app.feature.category.domain

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class CategoryContent(
    val category: Category?,
    val musics: List<Music>,
    val favoriteIds: Set<TrackId>,
    val playback: PlaybackSummary,
)

/**
 * Joins the three domain ports into the one thing a category screen shows. Feature-owned for the
 * same reason as the other screen-owned use cases: only the ports it reads are shared.
 */
internal class ObserveCategoryContentUseCase(
    private val soundPort: SoundPort,
    private val favoritesPort: FavoritesPort,
    private val playbackPort: PlaybackPort,
) {
    operator fun invoke(categoryId: CategoryId): Flow<CategoryContent> = combine(
        soundPort.observeCategory(categoryId),
        soundPort.observeMusicForCategory(categoryId),
        favoritesPort.favoriteIds,
        playbackPort.playback,
    ) { category, musics, favoriteIds, playback ->
        CategoryContent(category, musics, favoriteIds, playback)
    }
}
