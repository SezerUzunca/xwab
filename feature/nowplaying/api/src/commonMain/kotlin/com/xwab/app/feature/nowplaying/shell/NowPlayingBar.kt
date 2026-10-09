package com.xwab.app.feature.nowplaying.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.core.session.port.PlaybackItemId

/**
 * The strip the app shell draws under every tab: what is playing, its play/pause, and a way to its
 * own screen.
 *
 * The shell sees only this contract. The bar, its ViewModel and its strings stay in the feature's
 * implementation module, which contributes one to the navigation host's graph the way it would an
 * entry.
 */
interface NowPlayingBar {
    /**
     * @param onOpen opens the item's own screen; which screen that is, is the app's decision.
     * @param hiddenFor the item whose own screen is showing. That screen already has this item's
     *   play/pause and timer, so a bar for the same item would be a second copy of both. Null, or
     *   any other item, keeps the bar.
     */
    @Composable
    fun Content(
        onOpen: (PlaybackItemId) -> Unit,
        hiddenFor: PlaybackItemId?,
        modifier: Modifier,
    )
}
