package com.xwab.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.NavDisplay
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.di.AppGraph
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.TOP_LEVEL_DESTINATIONS
import com.xwab.app.navigation.rememberNavigationState
import com.xwab.app.navigation.toEntries
import com.xwab.app.ui.AppNavigationBar
import com.xwab.app.ui.NowPlayingBar
import kotlinx.coroutines.launch

/**
 * Shared application root used by the platform entry points.
 *
 * It applies the app theme and renders the application scaffold from the remembered navigation
 * state. The [graph] is built once by the platform entry point and handed down: nothing here looks
 * a dependency up.
 */
@Composable
fun App(graph: AppGraph) {
    SleepRelaxTheme {
        val navigationState = rememberNavigationState()
        val navigator = remember(navigationState) { Navigator(navigationState) }
        val entryProvider = remember(navigator, graph) {
            appEntryProvider(
                graph = graph,
                onNavigate = navigator::navigate,
                onBack = navigator::goBack,
            )
        }

        // The session outlives every screen, so the shell is where a control over it belongs. The
        // initial value is an empty summary rather than a suspend point: the bar is part of the
        // first frame and an idle session is exactly what it draws nothing for.
        val playback by graph.playbackPort.playback
            .collectAsStateWithLifecycle(initialValue = PlaybackSummary())
        // `PlaybackPort.play` must be resumed on the main thread; a composition scope is one.
        val scope = rememberCoroutineScope()

        Scaffold(
            // Feature screens paint their own gradient; this is only what shows behind the bar.
            containerColor = SleepRelaxTheme.colors.backgroundBottom,
            bottomBar = {
                Column {
                    playback.requestedItemId?.let { itemId ->
                        NowPlayingBar(
                            title = playback.title,
                            // The session's intent, so the icon and the tap read the same value —
                            // the rule every screen in this app already follows.
                            isPlaying = playback.playIntent,
                            isPreparing = playback.isPreparing,
                            onPlayPauseClick = {
                                if (playback.playIntent) {
                                    graph.playbackPort.pause()
                                } else {
                                    scope.launch { graph.playbackPort.play(itemId) }
                                }
                            },
                        )
                    }
                    AppNavigationBar(
                        destinations = TOP_LEVEL_DESTINATIONS,
                        selectedRoute = navigationState.topLevelRoute,
                        onSelect = navigator::navigate,
                    )
                }
            },
        ) { innerPadding ->
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = navigator::goBack,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
