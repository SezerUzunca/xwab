package com.xwab.app.ui

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEventBus
import com.xwab.app.composition.appEntryGraph
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.composition.appEntryMetadata
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.navigation.NavigationState
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.appNavigationState
import com.xwab.app.navigation.savedIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import androidx.navigation3.runtime.get

class TabEntryProviderTest {
    @Test
    fun theSameSoundInBrowseAndFavoritesHasIndependentDisplayIdentity() {
        val provider = appEntries()
        val state = NavigationState(
            startRoute = BrowseRoute,
            backStacks = mapOf(
                BrowseRoute to mutableListOf<NavKey>(BrowseRoute),
                FavoritesRoute to mutableListOf<NavKey>(FavoritesRoute),
            ),
        )
        val navigator = Navigator(state, ResultEventBus())
        navigator.navigate(CategoryRoute("rain"))
        navigator.navigate(SoundRoute("rain"))
        navigator.navigate(FavoritesRoute)
        navigator.navigate(SoundRoute("rain"))

        val entries = state.routesInUse.flatMap { tab ->
            state.backStacks.getValue(tab).map(tabEntries(tab, provider))
        }

        assertEquals(5, entries.size)
        assertEquals(entries.size, entries.map { it.contentKey }.toSet().size)
        assertNotEquals(entries[2].contentKey, entries[4].contentKey)
    }

    @Test
    fun recreatingProvidersAndRoutesKeepsTheSameSaveableIdentity() {
        val firstProvider = appEntries()
        val restoredProvider = appEntries()
        val first = tabEntries(FavoritesRoute, firstProvider)(SoundRoute("rain"))
        val restored = tabEntries(FavoritesRoute, restoredProvider)(SoundRoute("rain"))

        assertIs<String>(restored.contentKey)
        assertEquals(first.contentKey, restored.contentKey)
    }

    /** Saved UI state is keyed by the wire format, so renaming a route's class does not lose it. */
    @Test
    fun contentKeysFollowTheSavedNameRatherThanTheClassName() {
        val entry = tabEntries(FavoritesRoute, appEntries())(SoundRoute("rain"))

        val contentKey = entry.contentKey as String
        assertTrue(contentKey.contains("com.xwab.app.feature.favorites.navigation.FavoritesRoute"))
        assertTrue(contentKey.endsWith("com.xwab.app.feature.sound.navigation.SoundRoute|4:rain"))
    }

    @Test
    fun tabIdentityPreservesFeatureMetadataAndCustomContentKeys() {
        val metadata = mapOf<String, Any>("custom-scene" to true)
        fun provider(contentKey: String): (NavKey) -> NavEntry<NavKey> = { key ->
            NavEntry(key = key, contentKey = contentKey, metadata = metadata) {}
        }

        val first = tabEntries(BrowseRoute, provider("first"))(SoundRoute("rain"))
        val second = tabEntries(BrowseRoute, provider("second"))(SoundRoute("rain"))

        assertEquals(true, first.metadata["custom-scene"])
        assertEquals(BrowseRoute.savedIdentity(), first.metadata[TabKey])
        // A decorator cannot read the entry's key; the back arrow's decorator reads it from here.
        assertEquals(SoundRoute("rain"), first.metadata[DestinationKey])
        assertNotEquals(first.contentKey, second.contentKey)
    }
}

private fun appEntries() = appEntryProvider(appEntryGraph(appNavigationState()))

/** The production metadata; these tests are about identity, not what the back arrow does. */
private fun tabEntries(tab: NavKey, provider: (NavKey) -> NavEntry<NavKey>) =
    entryProviderForTab(tab, backStack = emptyList(), provider, ::appEntryMetadata)
