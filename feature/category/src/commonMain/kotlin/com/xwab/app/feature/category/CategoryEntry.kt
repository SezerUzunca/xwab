@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.category

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.sounds.navigation.navigateToPlayer
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Where this feature's routes turn into screens. */
internal fun EntryProviderScope<NavKey>.categoryEntry(navigator: Navigator) {
    entry<CategoryRoute> { route ->
        CategoryScreenRoute(
            // The route carries a plain id; the screen deals in `TrackId`. This is the seam.
            onMusicClick = { trackId -> navigator.navigateToPlayer(trackId.value) },
            onBack = navigator::goBack,
            viewModel = koinViewModel { parametersOf(route.categoryId) },
        )
    }
}
