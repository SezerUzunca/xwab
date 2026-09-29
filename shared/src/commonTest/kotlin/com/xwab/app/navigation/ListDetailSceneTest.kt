@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.xwab.app.navigation

import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import com.xwab.app.composition.appEntryMetadata
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.nowplaying.navigation.NowPlayingRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises the official Material strategy with the same tab metadata as the production display. */
class ListDetailSceneTest {
    private fun strategy(partitions: Int) = ListDetailSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = false,
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
        directive = PaneScaffoldDirective.Default.copy(maxHorizontalPartitions = partitions),
        adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
        paneExpansionDragHandle = null,
        paneExpansionState = null,
    )

    private fun calculate(entries: List<NavEntry<NavKey>>, partitions: Int = 2) =
        with(strategy(partitions)) { with(SceneStrategyScope<NavKey>()) { calculateScene(entries) } }

    private fun entries(tab: NavKey, vararg routes: NavKey): List<NavEntry<NavKey>> = routes.map(
        entryProviderForTab(tab, { route -> NavEntry(route) {} }, ::appEntryMetadata, onUp = {}),
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

    @Test
    fun detailChangesKeepTheSceneKeyAndBackPopsExactlyOneEntry() {
        val first = entries(StoriesRoute, StoriesRoute, StoryRoute("first"))
        val second = first + entries(StoriesRoute, StoryRoute("second"))
        assertEquals(assertNotNull(calculate(first)).key, assertNotNull(calculate(second)).key)
        assertEquals(first, assertNotNull(calculate(second)).previousEntries)
    }

    @Test
    fun everyPlayerTransitionHasAnOfficialMetadataOverride() {
        val metadata = appEntryMetadata(BrowseRoute, NowPlayingRoute)
        assertTrue(metadata[NavDisplay.TransitionKey] != null)
        assertTrue(metadata[NavDisplay.PopTransitionKey] != null)
        assertTrue(metadata[NavDisplay.PredictivePopTransitionKey] != null)
        assertNull(metadata[ParentPaneKey])
    }
}
