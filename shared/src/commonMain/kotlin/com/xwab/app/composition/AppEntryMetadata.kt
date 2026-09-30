@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.xwab.app.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.nowplaying.navigation.NowPlayingRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.ui.ParentPaneKey
import com.xwab.app.ui.playerTransitionMetadata
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.shared.generated.resources.Res
import xwab.shared.generated.resources.choose_category
import xwab.shared.generated.resources.choose_sound
import xwab.shared.generated.resources.choose_story

/**
 * Each tab is a distinct Material scene. Catalog's category and sound occupy detail and extra panes.
 *
 * @param beneath the tab's destinations below this one, oldest first; empty at a tab's root.
 */
internal fun appEntryMetadata(tab: NavKey, route: NavKey, beneath: List<NavKey>): Map<String, Any> {
    if (route == NowPlayingRoute) return playerTransitionMetadata()
    val sceneKey = tab.toString()
    return when (tab) {
        BrowseRoute -> catalogPaneMetadata(route, beneath, sceneKey)
        FavoritesRoute -> when (route) {
            FavoritesRoute -> listPane(sceneKey, Res.string.choose_sound)
            is SoundRoute -> ListDetailSceneStrategy.detailPane(sceneKey) + parentPane(ListDetailPaneScaffoldRole.List)
            else -> emptyMap()
        }
        StoriesRoute -> when (route) {
            StoriesRoute -> listPane(sceneKey, Res.string.choose_story)
            is StoryRoute -> ListDetailSceneStrategy.detailPane(sceneKey) + parentPane(ListDetailPaneScaffoldRole.List)
            else -> emptyMap()
        }
        else -> emptyMap()
    }
}

/**
 * A sound is the extra pane only beside the category it was opened from; earlier sounds chosen
 * there sit between them. The player opens one straight from the catalog's root: with no category
 * beneath, it is its own single pane, as a sound is in the stories tab, rather than an extra pane
 * with no detail beside it.
 */
private fun catalogPaneMetadata(route: NavKey, beneath: List<NavKey>, sceneKey: String): Map<String, Any> =
    when (route) {
        BrowseRoute -> listPane(sceneKey, Res.string.choose_category)
        is CategoryRoute -> ListDetailSceneStrategy.detailPane(sceneKey) + parentPane(ListDetailPaneScaffoldRole.List)
        is SoundRoute -> if (beneath.lastOrNull { it !is SoundRoute } is CategoryRoute) {
            ListDetailSceneStrategy.extraPane(sceneKey) + parentPane(ListDetailPaneScaffoldRole.Detail)
        } else {
            emptyMap()
        }
        else -> emptyMap()
    }

private fun parentPane(role: androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole) =
    metadata { put(ParentPaneKey, role) }

private fun listPane(sceneKey: String, hint: StringResource) = ListDetailSceneStrategy.listPane(sceneKey) {
    PanePlaceholder(hint)
}

@Composable
private fun PanePlaceholder(hint: StringResource) {
    ScreenContainer {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(hint), color = SleepRelaxTheme.colors.textSecondary)
        }
    }
}
