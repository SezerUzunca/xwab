package com.xwab.app.feature.browse.impl.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.feature.browse.api.navigation.BrowseRoute
import com.xwab.app.feature.browse.impl.BrowseScreenRoute
import com.xwab.app.feature.browse.impl.BrowseViewModel
import com.xwab.app.feature.browse.impl.di.BrowseDependencies

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
            viewModel = viewModel { BrowseViewModel(musicCatalog = dependencies.musicCatalog) },
        )
    }
}
