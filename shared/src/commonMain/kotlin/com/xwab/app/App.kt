package com.xwab.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.xwab.app.composition.AppNavigationHost
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.di.AppGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

/** Shared platform root: theme and application composition. */
@Composable
fun App(graph: AppGraph) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory) {
        SleepRelaxTheme {
            AppNavigationHost()
        }
    }
}
