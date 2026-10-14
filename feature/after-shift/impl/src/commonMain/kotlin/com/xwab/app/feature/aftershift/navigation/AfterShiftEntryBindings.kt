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
 * The After Shift tab's root. Its guides play in place and its note is kept in place; its three
 * intents, lying awake, rewriting a nightmare and a nap, go to the shell through its callback
 * contract.
 */
@ContributesTo(EntryProviderScope::class)
@BindingContainer
object AfterShiftEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: AfterShiftEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<AfterShiftRoute> {
            AfterShiftScreenRoute(
                onRewriteNightmare = callbacks.onRewriteNightmare,
                onCantSleep = callbacks.onCantSleep,
                onNap = callbacks.onNap,
                viewModel = metroViewModel(),
            )
        }
    }
}
