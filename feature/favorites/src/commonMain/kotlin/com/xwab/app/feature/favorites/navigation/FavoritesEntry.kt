package com.xwab.app.feature.favorites.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.favorites.FavoritesScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Actions supplied by the application's composition root. */
class FavoritesEntryCallbacks(
    val onTrackClick: (TrackId) -> Unit,
    val onBrowse: () -> Unit,
)

@ContributesTo(EntryProviderScope::class)
@BindingContainer
object FavoritesEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: FavoritesEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<FavoritesRoute> {
            FavoritesScreenRoute(
                onTrackClick = callbacks.onTrackClick,
                onBrowse = callbacks.onBrowse,
                viewModel = metroViewModel(),
            )
        }
    }
}
