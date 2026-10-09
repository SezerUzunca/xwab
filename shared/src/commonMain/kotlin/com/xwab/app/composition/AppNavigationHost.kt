package com.xwab.app.composition

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xwab.app.feature.nowplaying.shell.NowPlayingBar
import com.xwab.app.navigation.rememberNavigationState
import com.xwab.app.ui.AppNavigationDisplay
import com.xwab.app.ui.rememberTabEntries

/** Application wiring stays here; scenes, entry decorators and saved stacks live in navigation. */
@Composable
internal fun AppNavigationHost() {
    val state = rememberNavigationState()
    val graph = remember(state) { appEntryGraph(state) }
    val navigator = graph.navigator
    val provider = remember(graph) { appEntryProvider(graph) }
    AppNavigationDisplay(
        entries = rememberTabEntries(state, provider, ::appEntryMetadata, navigator::goUp, graph.resultEventBus),
        selectedTab = state.topLevelRoute,
        onSelectTab = navigator::selectTab,
        onBack = navigator::goBack,
        nowPlayingBar = {
            NowPlayingBar(
                onOpen = { item -> openPlaybackDetails(item, navigator::openInTab) },
                hiddenFor = state.currentBackStack.last().playbackItem(),
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}
