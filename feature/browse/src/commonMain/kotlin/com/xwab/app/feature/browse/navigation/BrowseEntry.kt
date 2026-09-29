package com.xwab.app.feature.browse.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.feature.browse.BrowseScreenRoute
import com.xwab.app.feature.browse.BrowseViewModel
import com.xwab.app.feature.browse.di.BrowseDependencies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.StringResource

/**
 * Where this feature's routes turn into screens.
 *
 * @param title the heading above the catalog; the app's name is the app's, so it is passed in.
 * @param subtitle the line under [title].
 */
fun EntryProviderScope<NavKey>.browseEntry(
    dependencies: () -> BrowseDependencies,
    title: StringResource,
    subtitle: StringResource,
    onCategoryClick: (CategoryId) -> Unit,
    reselectEvents: Flow<Unit> = emptyFlow(),
) {
    entry<BrowseRoute> {
        BrowseScreenRoute(
            title = title,
            subtitle = subtitle,
            onCategoryClick = onCategoryClick,
            reselectEvents = reselectEvents,
            // Built here rather than pulled from the graph: the ViewModel is internal to this
            // module, and `viewModel` scopes it to the entry's own store.
            viewModel = viewModel { BrowseViewModel(soundPort = dependencies().soundPort) },
        )
    }
}
