package com.xwab.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.xwab.app.composition.AppEntriesFactory
import com.xwab.app.composition.AppNavigationHost
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

/**
 * Shared platform root: theme and application composition.
 *
 * Takes the two things the composition root's graphs give the shell: the ViewModel factory entries
 * resolve their ViewModels from, and the factory for each navigation host's feature entries.
 */
@Composable
fun App(viewModelFactory: MetroViewModelFactory, entriesFactory: AppEntriesFactory) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
        SleepRelaxTheme {
            AppNavigationHost(entriesFactory)
        }
    }
}
