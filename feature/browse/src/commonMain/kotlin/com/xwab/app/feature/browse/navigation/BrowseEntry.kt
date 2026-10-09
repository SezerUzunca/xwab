package com.xwab.app.feature.browse.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.feature.browse.BrowseScreenRoute
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource

/** Application-owned labels and actions; this feature does not name destination features. */
class BrowseEntryCallbacks(
    val title: StringResource,
    val subtitle: StringResource,
    val onCategoryClick: (CategoryId) -> Unit,
)

@ContributesTo(EntryProviderScope::class)
@BindingContainer
object BrowseEntryBindings {
    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(
        callbacks: BrowseEntryCallbacks,
    ): EntryProviderScope<NavKey>.() -> Unit = {
        entry<BrowseRoute> {
            BrowseScreenRoute(
                title = callbacks.title,
                subtitle = callbacks.subtitle,
                onCategoryClick = callbacks.onCategoryClick,
                viewModel = metroViewModel(),
            )
        }
    }
}
