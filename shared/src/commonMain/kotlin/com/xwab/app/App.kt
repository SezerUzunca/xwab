package com.xwab.app

import androidx.compose.runtime.Composable
import com.xwab.app.composition.AppNavigationHost
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.di.AppGraph

/** Shared platform root: theme and application composition. */
@Composable
fun App(graph: AppGraph) {
    SleepRelaxTheme {
        AppNavigationHost(graph)
    }
}
