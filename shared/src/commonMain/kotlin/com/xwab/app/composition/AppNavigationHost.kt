package com.xwab.app.composition

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.nowplaying.navigation.NowPlayingRoute
import com.xwab.app.feature.nowplaying.shell.NowPlayingBar
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.rememberNavigationState
import com.xwab.app.ui.AppNavigationDisplay
import com.xwab.app.ui.rememberTabEntries

/** Application wiring stays here; scenes, entry decorators and saved stacks live in navigation. */
@Composable
internal fun AppNavigationHost(graph: AppGraph) {
    val state = rememberNavigationState()
    val navigator = remember(state) { Navigator(state) }
    val provider = remember(navigator, graph) {
        appEntryProvider(
            graph, navigator::navigate, navigator::goBack, navigator::replaceCurrent, navigator::reselections,
        )
    }
    AppNavigationDisplay(
        entries = rememberTabEntries(state, provider, ::appEntryMetadata, navigator::goUp),
        selectedTab = state.topLevelRoute,
        onSelectTab = navigator::selectTab,
        onBack = navigator::goBack,
        player = {
            val current = state.currentBackStack.last()
            if (current != NowPlayingRoute) {
                NowPlayingBar(
                    graph.nowPlayingDependencies,
                    onOpen = { navigator.navigate(NowPlayingRoute) },
                    hiddenFor = current.playbackItem(),
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
