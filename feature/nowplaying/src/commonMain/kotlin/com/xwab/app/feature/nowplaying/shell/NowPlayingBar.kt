package com.xwab.app.feature.nowplaying.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.feature.nowplaying.NowPlayingMiniPlayerRoute
import com.xwab.app.feature.nowplaying.NowPlayingViewModel
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies
import com.xwab.app.feature.nowplaying.domain.ObserveNowPlayingContentUseCase

/**
 * Persistent mini player; the app shell owns opening this feature's destination.
 *
 * @param hiddenFor the item whose own screen is showing. That screen already has this item's
 *   play/pause and timer, so a bar for the same item would be a second copy of both. Null, or any
 *   other item, keeps the bar.
 */
@Composable
fun NowPlayingBar(
    dependencies: () -> NowPlayingDependencies,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    hiddenFor: PlaybackItemId? = null,
) {
    NowPlayingMiniPlayerRoute(
        onOpen = onOpen,
        hiddenFor = hiddenFor,
        modifier = modifier,
        viewModel = viewModel {
            val ports = dependencies()
            NowPlayingViewModel(ObserveNowPlayingContentUseCase(ports.playbackPort), ports.playbackPort)
        },
    )
}
