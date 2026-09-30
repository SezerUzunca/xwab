@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.xwab.app.composition

import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.nowplaying.navigation.NowPlayingRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.ui.ParentPaneKey
import com.xwab.app.ui.entryProviderForTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises the official Material strategy with the same tab metadata as the production display. */
class AppEntryMetadataTest {
    private fun strategy(partitions: Int) = ListDetailSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = false,
        backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
        directive = PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = partitions),
        adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
        paneExpansionDragHandle = null,
        paneExpansionState = null,
    )

    private fun calculate(entries: List<NavEntry<NavKey>>, partitions: Int = 2) =
        with(strategy(partitions)) { with(SceneStrategyScope<NavKey>()) { calculateScene(entries) } }

    /** One tab's stack, in order; each entry's metadata sees the destinations beneath it. */
    private fun entries(tab: NavKey, vararg routes: NavKey): List<NavEntry<NavKey>> = routes.map(
        entryProviderForTab(tab, routes.toList(), { route -> NavEntry(route) {} }, ::appEntryMetadata),
    )

    @Test
    fun compactWindowsUseTheSinglePaneFallback() {
        val entries = entries(BrowseRoute, BrowseRoute, CategoryRoute("rain"), SoundRoute("rain"))
        assertNull(calculate(entries, partitions = 1))
    }

    @Test
    fun catalogListDetailAndExtraPanesBelongToOneOfficialScene() {
        val entries = entries(BrowseRoute, BrowseRoute, CategoryRoute("rain"), SoundRoute("rain"))
        val scene = assertNotNull(calculate(entries, partitions = 3))
        assertEquals(entries, scene.entries)
        assertEquals(entries.dropLast(1), scene.previousEntries)
    }

    @Test
    fun favoritesAndStoriesEachKeepTheirOwnSceneIdentity() {
        val favorites = entries(FavoritesRoute, FavoritesRoute, SoundRoute("rain"))
        val stories = entries(StoriesRoute, StoriesRoute, StoryRoute("bedtime"))
        assertEquals(FavoritesRoute.toString(), assertNotNull(calculate(favorites)).key)
        assertEquals(StoriesRoute.toString(), assertNotNull(calculate(stories)).key)
    }

    @Test
    fun anotherTabsPreservedListCannotJoinTheSelectedTabsScene() {
        val browse = entries(BrowseRoute, BrowseRoute, CategoryRoute("rain"), SoundRoute("rain"))
        val favorites = entries(FavoritesRoute, FavoritesRoute, SoundRoute("rain"))
        val scene = assertNotNull(calculate(browse + favorites))
        assertEquals(favorites, scene.entries)
        assertEquals(browse + favorites.dropLast(1), scene.previousEntries)
    }

    @Test
    fun unrelatedContentKindsAndPlayerUseSinglePane() {
        assertNull(calculate(entries(FavoritesRoute, FavoritesRoute, StoryRoute("bedtime"))))
        assertNull(calculate(entries(StoriesRoute, StoriesRoute, SoundRoute("rain"))))
        assertNull(calculate(entries(BrowseRoute, BrowseRoute, CategoryRoute("rain"), NowPlayingRoute)))
    }

    @Test
    fun wideRootsHaveAnOfficialDetailPlaceholder() {
        val entries = entries(StoriesRoute, StoriesRoute)
        assertEquals(entries, assertNotNull(calculate(entries)).entries)
    }

    /** Choosing one story after another beside the list: one Back returns to the list. */
    @Test
    fun earlierSelectionsInThePaneAreSkippedByOneBack() {
        val first = entries(StoriesRoute, StoriesRoute, StoryRoute("first"))
        val stack = entries(StoriesRoute, StoriesRoute, StoryRoute("first"), StoryRoute("second"), StoryRoute("third"))
        val scene = assertNotNull(calculate(stack))
        assertEquals(assertNotNull(calculate(first)).key, scene.key)
        assertEquals(stack.take(1), scene.previousEntries)
    }

    /** Another sound chosen in the category stays the extra pane; Back returns to the category. */
    @Test
    fun soundsChosenInTurnInACategoryShareTheExtraPane() {
        val stack = entries(BrowseRoute, BrowseRoute, CategoryRoute("rain"), SoundRoute("a"), SoundRoute("b"))
        assertEquals(ListDetailPaneScaffoldRole.Detail, stack.last().metadata[ParentPaneKey])
        assertEquals(stack.take(2), assertNotNull(calculate(stack, partitions = 3)).previousEntries)
    }

    /** The player's "View details" opens a sound straight from the catalog root, with no category. */
    @Test
    fun aSoundWithNoCategoryBeneathIsASinglePaneNotAnOrphanedExtraPane() {
        val fromPlayer = entries(BrowseRoute, BrowseRoute, SoundRoute("rain"))
        assertNull(calculate(fromPlayer, partitions = 3))
        assertNull(fromPlayer.last().metadata[ParentPaneKey])

        val fromCategory = entries(BrowseRoute, BrowseRoute, CategoryRoute("rain"), SoundRoute("rain"))
        assertEquals(ListDetailPaneScaffoldRole.Detail, fromCategory.last().metadata[ParentPaneKey])
    }

    @Test
    fun everyPlayerTransitionHasAnOfficialMetadataOverride() {
        val metadata = appEntryMetadata(BrowseRoute, NowPlayingRoute, beneath = listOf(BrowseRoute))
        assertTrue(metadata[NavDisplay.TransitionKey] != null)
        assertTrue(metadata[NavDisplay.PopTransitionKey] != null)
        assertTrue(metadata[NavDisplay.PredictivePopTransitionKey] != null)
        assertNull(metadata[ParentPaneKey])
    }
}
