package com.xwab.app.feature.category.impl

import com.arkivanov.decompose.ComponentContext
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.navigation.componentScope
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackKind
import com.xwab.app.core.playbacksession.requestedValueOf
import com.xwab.app.core.ui.state.Loadable
import com.xwab.app.feature.category.impl.domain.CategoryContent
import com.xwab.app.feature.category.impl.domain.ObserveCategoryContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface CategoryComponent {
    val state: StateFlow<Loadable<CategoryState>>
    val onMusicClick: (TrackId) -> Unit
    val onBack: () -> Unit
    fun toggleFavorite(musicId: TrackId)
    fun togglePlayback(musicId: TrackId)
}

internal class DefaultCategoryComponent(
    componentContext: ComponentContext,
    categoryId: CategoryId,
    observeCategoryContentUseCase: ObserveCategoryContentUseCase,
    private val favoritesRepository: FavoritesRepository,
    private val playbackCoordinator: PlaybackCoordinator,
    override val onMusicClick: (TrackId) -> Unit,
    override val onBack: () -> Unit,
) : CategoryComponent, ComponentContext by componentContext {
    private val scope = componentScope()

    override val state: StateFlow<Loadable<CategoryState>> = observeCategoryContentUseCase(categoryId)
        .map<CategoryContent, Loadable<CategoryState>> { content ->
            Loadable.Ready(
                CategoryState(
                    category = content.category,
                    musics = content.musics,
                    favoriteIds = content.favoriteIds,
                    // A story occupying the session lights up no row on a screen that lists sounds.
                    requestedTrackId = content.playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId),
                    playIntent = content.playback.playIntent,
                ),
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )

    override fun toggleFavorite(musicId: TrackId) {
        scope.launch { favoritesRepository.toggle(musicId) }
    }

    /** Branches on the value the control renders, so the icon and the tap cannot disagree. */
    override fun togglePlayback(musicId: TrackId) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.requestedTrackId == musicId && current.playIntent) {
            playbackCoordinator.pause()
        } else {
            scope.launch { playbackCoordinator.play(PlaybackItemId.sound(musicId.value)) }
        }
    }
}
