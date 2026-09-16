package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * This feature's state and its one action — the job a ViewModel does for every other screen here.
 *
 * ## Why this is not a `ViewModel`
 *
 * Every feature ViewModel in this app is created by `viewModel { }` inside an `entry<Route> { }`,
 * which resolves its store through `rememberViewModelStoreNavEntryDecorator`: one store per
 * navigation entry, retained with that entry's place on the back stack. This feature has no entry.
 * It is chrome, drawn outside `NavDisplay` so that it survives every destination change, so there
 * is no navigation entry to scope a store to.
 *
 * `viewModel { }` would still resolve *something* — the Activity on Android, the view controller on
 * iOS — but that is a different owner with a different lifetime from the one the rest of this build
 * means by "the feature's ViewModel", and whether the multiplatform half provides it at all is not
 * a thing a compiler would tell us: a missing `LocalViewModelStoreOwner` throws at runtime, on the
 * platform this project cannot run locally. A green build and a crash on launch is the one outcome
 * worth designing away from.
 *
 * Nothing is lost by not being one. `viewModelScope` would buy a scope that outlives composition,
 * and the bar is in composition for as long as the app is; `stateIn(WhileSubscribed)` would cache a
 * session flow that is already hot and app-scoped. What a ViewModel is really here for — keeping
 * the state mapping and the tap decision out of the composable, where nothing can test them — this
 * does, and with less machinery: [togglePlayback] is a plain suspend function over a port.
 */
internal class NowPlayingPresenter(private val playbackPort: PlaybackPort) {

    val state: Flow<NowPlayingState> = playbackPort.playback.map { it.toNowPlayingState() }

    /**
     * Branches on the same value the control renders, which is the rule every screen in this app
     * follows: whatever the icon says, the tap does.
     *
     * Takes the state rather than reading it back, so the decision is made against exactly what the
     * listener was looking at when they tapped.
     */
    suspend fun togglePlayback(current: NowPlayingState) {
        val itemId = current.itemId ?: return
        if (current.playIntent) playbackPort.pause() else playbackPort.play(itemId)
    }
}
