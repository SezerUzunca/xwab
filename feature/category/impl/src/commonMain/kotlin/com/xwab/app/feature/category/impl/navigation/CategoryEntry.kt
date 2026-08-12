@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.category.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.feature.category.api.navigation.CategoryRoute
import com.xwab.app.feature.category.impl.CategoryScreenRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.categoryEntry(
    onMusicClick: (TrackId) -> Unit,
    onBack: () -> Unit,
) {
    entry<CategoryRoute> { route ->
        CategoryScreenRoute(
            onMusicClick = onMusicClick,
            onBack = onBack,
            viewModel = koinViewModel { parametersOf(CategoryId(route.categoryId)) },
        )
    }
}
