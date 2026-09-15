package com.xwab.app.feature.category.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.feature.category.CategoryScreenRoute
import com.xwab.app.feature.category.CategoryViewModel
import com.xwab.app.feature.category.di.CategoryDependencies
import com.xwab.app.feature.category.domain.ObserveCategoryContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.categoryEntry(
    dependencies: () -> CategoryDependencies,
    onTrackClick: (TrackId) -> Unit,
    onBack: () -> Unit,
) {
    entry<CategoryRoute> { route ->
        CategoryScreenRoute(
            onTrackClick = onTrackClick,
            onBack = onBack,
            // A route is a serialized wire format, so it carries the plain id and the wrapper goes
            // back on here — the one place this feature handles a bare category string.
            viewModel = viewModel {
                val ports = dependencies()
                CategoryViewModel(
                    categoryId = CategoryId(route.categoryId),
                    observeCategoryContentUseCase = ObserveCategoryContentUseCase(
                        ports.soundPort,
                        ports.favoritesPort,
                        ports.playbackPort,
                    ),
                    favoritesPort = ports.favoritesPort,
                    playbackPort = ports.playbackPort,
                )
            },
        )
    }
}
