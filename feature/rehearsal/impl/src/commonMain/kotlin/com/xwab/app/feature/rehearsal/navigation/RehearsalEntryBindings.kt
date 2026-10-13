package com.xwab.app.feature.rehearsal.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.rehearsal.RehearsalScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** The one screen where the listener writes and rehearses their rewritten nightmare. */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object RehearsalEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: RehearsalEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<RehearsalRoute> {
            RehearsalScreenRoute(onBack = callbacks.onBack, viewModel = metroViewModel())
        }
    }
}
