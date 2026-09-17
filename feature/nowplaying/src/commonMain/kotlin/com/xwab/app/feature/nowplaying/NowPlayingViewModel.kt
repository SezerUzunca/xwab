package com.xwab.app.feature.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwab.app.core.session.port.PlaybackPort
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A ViewModel like every other feature's, with one difference worth knowing.
 *
 * The others are scoped to a navigation entry: `rememberViewModelStoreNavEntryDecorator` opens a
 * child store per entry and clears it when that entry is popped. This feature has no entry — it is
 * chrome, drawn outside `NavDisplay` so that it outlives every destination change — so `viewModel`
 * resolves the owner the decorator itself asks for: the root one. Which is the right lifetime here.
 * The bar is one thing, present from the first frame to the last, over a session that is app-scoped
 * too; there is no entry for it to be cleared along with.
 *
 * That the root owner has to exist is not a new requirement this adds. The app shell already calls
 * `rememberViewModelStoreNavEntryDecorator()` with no arguments, and that function is
 * `checkNotNull(LocalViewModelStoreOwner.current)` — every screen in this app already depends on it.
 */
internal class NowPlayingViewModel(
    private val playbackPort: PlaybackPort,
) : ViewModel() {

    val state: StateFlow<NowPlayingState> = playbackPort.playback
        .map { it.toNowPlayingState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NowPlayingState(),
        )

    /**
     * Branches on the value the control renders, so the icon and the tap cannot disagree — the rule
     * every screen in this app follows.
     */
    fun togglePlayback() {
        val current = state.value
        val itemId = current.itemId ?: return
        if (current.playIntent) {
            playbackPort.pause()
        } else {
            viewModelScope.launch { playbackPort.play(itemId) }
        }
    }
}
