package com.xwab.app.feature.category.domain

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.SOUND_FAVORITES_NAMESPACE
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal enum class CategoryFavoritesReadStatus {
    Pending,
    Available,
    Unavailable,
}

internal data class CategoryContent(
    val category: Category?,
    val tracks: List<Track>,
    val favoriteIds: Set<TrackId>,
    val playback: PlaybackSummary,
    val favoritesReadStatus: CategoryFavoritesReadStatus,
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
    private fun categoryTracks(categoryId: CategoryId): Flow<CategoryTracks> = combine(
        soundPort.observeCategory(categoryId),
        soundPort.observeTracksForCategory(categoryId),
        favoritesPort.observe(SOUND_FAVORITES_NAMESPACE)
            .map {
                FavoriteStatus(
                    it.ids.mapTo(mutableSetOf(), ::TrackId),
                    if (it.isAvailable) CategoryFavoritesReadStatus.Available else CategoryFavoritesReadStatus.Unavailable,
                )
            }
            .onStart { emit(FavoriteStatus(emptySet(), CategoryFavoritesReadStatus.Pending)) }
            .distinctUntilChanged(),
    ) { category, tracks, favorites ->
        CategoryTracks(
            category = category,
            tracks = tracks,
            favoriteIds = tracks.mapNotNullTo(mutableSetOf()) { it.id.takeIf(favorites.ids::contains) },
            favoritesReadStatus = favorites.readStatus,
        )
    }.distinctUntilChanged()

    operator fun invoke(categoryId: CategoryId): Flow<CategoryContent> = combine(
        categoryTracks(categoryId),
        playbackPort.playback,
    ) { category, playback ->
        CategoryContent(category.category, category.tracks, category.favoriteIds, playback, category.favoritesReadStatus)
    }

    private data class FavoriteStatus(val ids: Set<TrackId>, val readStatus: CategoryFavoritesReadStatus)

    private data class CategoryTracks(
        val category: Category?,
        val tracks: List<Track>,
        val favoriteIds: Set<TrackId>,
        val favoritesReadStatus: CategoryFavoritesReadStatus,
    )
}
