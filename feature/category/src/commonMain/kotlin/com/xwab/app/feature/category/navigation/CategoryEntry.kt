package com.xwab.app.feature.category.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.category.CategoryScreenRoute
import com.xwab.app.feature.category.CategoryViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

/** Actions supplied by the application's composition root. */
class CategoryEntryCallbacks(
    val onTrackClick: (TrackId) -> Unit,
    val onBack: () -> Unit,
)

@ContributesTo(EntryProviderScope::class)
@BindingContainer
object CategoryEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: CategoryEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<CategoryRoute> { route ->
            CategoryScreenRoute(
                onTrackClick = callbacks.onTrackClick,
                onBack = callbacks.onBack,
                // A route is a serialized wire format, so it carries the plain id and the wrapper goes
                // back on here — the one place this feature handles a bare category string.
                viewModel = assistedMetroViewModel<CategoryViewModel, CategoryViewModel.Factory> {
                    create(CategoryId(route.categoryId))
                },
            )
        }
    }
}
