package com.xwab.app.composition

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.xwab.app.feature.browse.impl.BrowseScreenRoute
import com.xwab.app.feature.category.impl.CategoryScreenRoute
import com.xwab.app.feature.favorites.impl.FavoritesScreenRoute
import com.xwab.app.feature.sounds.impl.PlayerScreenRoute
import com.xwab.app.feature.story.impl.StoriesScreenRoute
import com.xwab.app.navigation.AppTab

/** Renders the selected tab's stack. Where a tab's children turn into screens. */
@Composable
internal fun AppContent(component: AppComponent, selectedTab: AppTab, modifier: Modifier = Modifier) {
    when (selectedTab) {
        AppTab.BROWSE -> Children(
            stack = component.browseStack,
            modifier = modifier,
            animation = stackAnimation(fade() + scale()),
        ) { child ->
            when (val instance = child.instance) {
                is BrowseTabChild.Root -> BrowseScreenRoute(instance.component)
                is BrowseTabChild.Category -> CategoryScreenRoute(instance.component)
                is BrowseTabChild.Player -> PlayerScreenRoute(instance.component)
            }
        }

        AppTab.FAVORITES -> Children(
            stack = component.favoritesStack,
            modifier = modifier,
            animation = stackAnimation(fade() + scale()),
        ) { child ->
            when (val instance = child.instance) {
                is FavoritesTabChild.Root -> FavoritesScreenRoute(instance.component)
                is FavoritesTabChild.Player -> PlayerScreenRoute(instance.component)
            }
        }

        AppTab.STORIES -> Children(
            stack = component.storiesStack,
            modifier = modifier,
            animation = stackAnimation(fade() + scale()),
        ) { child ->
            when (val instance = child.instance) {
                is StoriesTabChild.Root -> StoriesScreenRoute(instance.component)
            }
        }
    }
}
