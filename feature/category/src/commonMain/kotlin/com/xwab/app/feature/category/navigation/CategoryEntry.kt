package com.xwab.app.feature.category.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.category.CategoryScreenRoute
import com.xwab.app.feature.category.CategoryViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.categoryEntry(
    onTrackClick: (TrackId) -> Unit,
    onBack: () -> Unit,
) {
    entry<CategoryRoute> { route ->
        CategoryScreenRoute(
            onTrackClick = onTrackClick,
            onBack = onBack,
            // A route is a serialized wire format, so it carries the plain id and the wrapper goes
            // back on here — the one place this feature handles a bare category string.
            viewModel = assistedMetroViewModel<CategoryViewModel, CategoryViewModel.Factory> {
                create(CategoryId(route.categoryId))
            },
        )
    }
}
