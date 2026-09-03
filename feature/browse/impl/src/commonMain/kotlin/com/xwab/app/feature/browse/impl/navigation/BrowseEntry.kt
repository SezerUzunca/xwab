@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package com.xwab.app.feature.browse.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.feature.browse.api.navigation.BrowseRoute
import com.xwab.app.feature.browse.impl.BrowseScreenRoute
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.browseEntry(onCategoryClick: (CategoryId) -> Unit) {
    entry<BrowseRoute> {
        BrowseScreenRoute(
            onCategoryClick = onCategoryClick,
            viewModel = koinViewModel(),
        )
    }
}
