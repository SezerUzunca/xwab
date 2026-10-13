package com.xwab.app.feature.aftershift.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.aftershift.AfterShiftScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The After Shift tab's one screen. It sends no intents of its own: its guides play in place and
 * its note is kept in place, so there is no callback contract for the shell to supply.
 */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object AfterShiftEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderScope<NavKey>.() -> Unit = {
        entry<AfterShiftRoute> {
            AfterShiftScreenRoute(viewModel = metroViewModel())
        }
    }
}
