package com.xwab.app.feature.category.impl.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.category.api.navigation.CategoryRoute
import com.xwab.app.feature.category.impl.CategoryScreenRoute
import com.xwab.app.feature.category.impl.CategoryViewModel
import com.xwab.app.feature.category.impl.di.CategoryDependencies
import com.xwab.app.feature.category.impl.domain.ObserveCategoryContentUseCase

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.categoryEntry(
    dependencies: CategoryDependencies,
    onMusicClick: (TrackId) -> Unit,
    onBack: () -> Unit,
) {
    entry<CategoryRoute> { route ->
        CategoryScreenRoute(
            onMusicClick = onMusicClick,
            onBack = onBack,
            // A route is a serialized wire format, so it carries the plain id and the wrapper goes
            // back on here — the one place this feature handles a bare category string.
            viewModel = viewModel {
                CategoryViewModel(
                    categoryId = CategoryId(route.categoryId),
                    observeCategoryContentUseCase = ObserveCategoryContentUseCase(
                        dependencies.musicCatalog,
                        dependencies.favoritesRepository,
                        dependencies.playbackCoordinator,
                    ),
                    favoritesRepository = dependencies.favoritesRepository,
                    playbackCoordinator = dependencies.playbackCoordinator,
                )
            },
        )
    }
}
