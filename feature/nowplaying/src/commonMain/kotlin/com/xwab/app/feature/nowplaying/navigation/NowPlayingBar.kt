package com.xwab.app.feature.nowplaying.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.feature.nowplaying.NowPlayingScreen
import com.xwab.app.feature.nowplaying.NowPlayingViewModel
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies

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
 * @param dependencies invoked once, inside the ViewModel initializer and not while the shell is
 *   assembling its scaffold — the same rule every other feature's entry follows.
 * @param onOpen a tap on the bar, carrying the item it is holding. Where that leads is application
 *   policy and is decided by the shell: this feature knows only that a sound and a story are
 *   different kinds of thing, never which screen either one has. The same shape as `onTrackClick`
 *   on every other entry.
 */
@Composable
fun NowPlayingBar(
    dependencies: () -> NowPlayingDependencies,
    onOpen: (PlaybackItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel { NowPlayingViewModel(dependencies().playbackPort) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    NowPlayingScreen(
        state = state,
        onPlayPauseClick = viewModel::togglePlayback,
        // The bar is not drawn at all while the session is idle, so this can only be null on a tap
        // racing the state it was drawn from — the same race `togglePlayback` guards against.
        onOpenClick = { state.itemId?.let(onOpen) },
        modifier = modifier,
    )
}
