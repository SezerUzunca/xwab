package com.xwab.app.feature.cantsleep.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.cantsleep.CantSleepScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** The one screen for lying awake: what to do, and the guides to hear out of bed. */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object CantSleepEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: CantSleepEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<CantSleepRoute> {
            CantSleepScreenRoute(onBack = callbacks.onBack, viewModel = metroViewModel())
        }
    }
}
