package com.xwab.app.feature.favorites.impl

import com.arkivanov.decompose.ComponentContext
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.navigation.componentScope
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackItemId
import com.xwab.app.core.playbacksession.PlaybackKind
import com.xwab.app.core.playbacksession.requestedValueOf
import com.xwab.app.core.ui.state.Loadable
import com.xwab.app.feature.favorites.impl.domain.FavoritesContent
import com.xwab.app.feature.favorites.impl.domain.ObserveFavoritesContentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface FavoritesComponent {
    val state: StateFlow<Loadable<FavoritesState>>
    val onMusicClick: (TrackId) -> Unit
    fun togglePlayback(musicId: TrackId)
}

internal class DefaultFavoritesComponent(
    componentContext: ComponentContext,
    observeFavoritesContentUseCase: ObserveFavoritesContentUseCase,
    private val playbackCoordinator: PlaybackCoordinator,
    override val onMusicClick: (TrackId) -> Unit,
) : FavoritesComponent, ComponentContext by componentContext {
    private val scope = componentScope()

    override val state: StateFlow<Loadable<FavoritesState>> = observeFavoritesContentUseCase()
        .map<FavoritesContent, Loadable<FavoritesState>> { content ->
            val playback = content.playback
            val requestedTrackId = playback.requestedValueOf(PlaybackKind.SOUND)?.let(::TrackId)
            val failure = playback.failure?.takeIf { it.itemId.kind == PlaybackKind.SOUND }
            Loadable.Ready(
                FavoritesState(
                    musics = content.musics,
                    requestedTrackId = requestedTrackId,
                    playIntent = requestedTrackId != null && playback.playIntent,
                    isPreparing = requestedTrackId != null && playback.isPreparing,
                    playbackFailure = failure,
                ),
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Loadable.Loading,
        )

    override fun togglePlayback(musicId: TrackId) {
        val current = (state.value as? Loadable.Ready)?.value ?: return
        if (current.requestedTrackId == musicId && current.playIntent) {
            playbackCoordinator.pause()
        } else {
            scope.launch { playbackCoordinator.play(PlaybackItemId.sound(musicId.value)) }
        }
    }
}
