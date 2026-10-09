package com.xwab.app.feature.nowplaying.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import com.xwab.app.core.session.port.PlaybackItemId
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Hands the shell its [NowPlayingBar] through the navigation host's graph, beside the features'
 * entry installers, so the shell compiles against the contract alone.
 */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object NowPlayingBarBindings {
    @Provides
    fun provideNowPlayingBar(): NowPlayingBar = ViewModelNowPlayingBar
}

private object ViewModelNowPlayingBar : NowPlayingBar {
    @Composable
    override fun Content(onOpen: (PlaybackItemId) -> Unit, hiddenFor: PlaybackItemId?, modifier: Modifier) {
        NowPlayingBarRoute(onOpen = onOpen, modifier = modifier, hiddenFor = hiddenFor)
    }
}
