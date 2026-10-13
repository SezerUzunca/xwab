package com.xwab.app.feature.shuffle.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.shuffle.ShuffleScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The shuffle tab's one screen. It sends no intents of its own: the decks play in place, so there
 * is no callback contract for the shell to supply.
 */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object ShuffleEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<ShuffleRoute> {
            ShuffleScreenRoute(viewModel = metroViewModel())
        }
    }
}
