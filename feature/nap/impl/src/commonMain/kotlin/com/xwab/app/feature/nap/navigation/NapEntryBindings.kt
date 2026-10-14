package com.xwab.app.feature.nap.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.nap.NapScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** The one screen for naps: each length with its play/pause, and what a nap here is not. */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object NapEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: NapEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<NapRoute> {
            NapScreenRoute(onBack = callbacks.onBack, viewModel = metroViewModel())
        }
    }
}
