package com.xwab.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.xwab.app.composition.AppNavigationHost
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

/**
 * Shared platform root: theme and application composition.
 *
 * Takes the application graph's ViewModel factory, the one thing composition reads from it.
 */
@Composable
fun App(viewModelFactory: MetroViewModelFactory) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
        SleepRelaxTheme {
            AppNavigationHost()
        }
    }
}
