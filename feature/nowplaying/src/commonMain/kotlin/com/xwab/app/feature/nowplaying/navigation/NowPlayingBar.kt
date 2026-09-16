package com.xwab.app.feature.nowplaying.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xwab.app.feature.nowplaying.NowPlayingPresenter
import com.xwab.app.feature.nowplaying.NowPlayingScreen
import com.xwab.app.feature.nowplaying.NowPlayingState
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies
import kotlinx.coroutines.launch

/**
 * This feature's whole public surface, and the app shell's only way in.
 *
 * It lives in `.navigation` because that is where this build puts a feature's public contract —
 * the package a shell is allowed to reach and the only place a feature may expose anything. It is
 * a composable rather than a route because this feature is chrome, not a destination: the shell
 * places it in its scaffold, outside `NavDisplay`, so that it outlives every destination change.
 * The contract is otherwise the same shape as `soundEntry` or `browseEntry` — the shell hands over
 * a dependency provider and nothing else crosses.
 *
 * No other feature knows this exists. Whatever a listener started, from wherever, reaches this bar
 * through the session alone.
 *
 * @param dependencies invoked once, on first composition, and not while the shell is merely
 *   assembling its scaffold — the same reason every other feature invokes its provider inside a
 *   ViewModel initializer rather than at registration.
 */
@Composable
fun NowPlayingBar(
    dependencies: () -> NowPlayingDependencies,
    modifier: Modifier = Modifier,
) {
    // Keyed on nothing on purpose. The graph is built once per process and its provider is not a
    // value a screen recomposes over; keying on the lambda would rebuild the presenter — and
    // restart its collection — on every recomposition that handed back a fresh function object.
    val presenter = remember { NowPlayingPresenter(dependencies().playbackPort) }
    val state by presenter.state.collectAsStateWithLifecycle(initialValue = NowPlayingState())
    // `PlaybackPort.play` must be resumed on the main thread; a composition scope is one. Taken
    // here rather than inside the bar's own visibility branch, so that a lookup started by the last
    // tap is not cancelled by the bar going away.
    val scope = rememberCoroutineScope()

    NowPlayingScreen(
        state = state,
        onPlayPauseClick = { scope.launch { presenter.togglePlayback(state) } },
        modifier = modifier,
    )
}
