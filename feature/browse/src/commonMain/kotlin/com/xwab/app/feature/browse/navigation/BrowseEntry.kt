package com.xwab.app.feature.browse.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.browse.BrowseScreenRoute
import com.xwab.app.feature.browse.BrowseViewModel
import com.xwab.app.feature.browse.di.BrowseDependencies

/** Where this feature's routes turn into screens. */
fun EntryProviderScope<NavKey>.browseEntry(
    dependencies: BrowseDependencies,
    onCategoryClick: (CategoryId) -> Unit,
) {
    entry<BrowseRoute> {
        BrowseScreenRoute(
            onCategoryClick = onCategoryClick,
            // Built here rather than pulled from the graph: the ViewModel is internal to this
            // module, and `viewModel` scopes it to the entry's own store.
            viewModel = viewModel { BrowseViewModel(soundCatalogPort = dependencies.soundCatalogPort) },
        )
    }
}
