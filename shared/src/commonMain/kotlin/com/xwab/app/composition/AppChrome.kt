package com.xwab.app.composition

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.nowplaying.navigation.NowPlayingBar

/**
 * Where feature *chrome* is connected to the app shell, beside [appEntryProvider], which connects
 * feature *destinations*.
 *
 * Both live in the composition root for the same reason and under the same rule: this is one of the
 * two packages allowed to name a feature's navigation contract, and the app root itself is not.
 * Keeping the reference here is what lets `App` place the bar without knowing which feature draws
 * it — or that a feature draws it at all.
 *
 * The graph's provider is passed straight through rather than invoked: this feature initializes its
 * own ports on first composition, and the shell is not the one to decide when that is.
 */
@Composable
internal fun AppNowPlayingBar(graph: AppGraph, modifier: Modifier = Modifier) {
    NowPlayingBar(dependencies = graph.nowPlayingDependencies, modifier = modifier)
}
