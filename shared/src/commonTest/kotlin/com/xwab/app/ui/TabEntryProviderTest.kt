package com.xwab.app.ui

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.composition.appEntryMetadata
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.browse.di.BrowseDependencies
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.di.CategoryDependencies
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.di.FavoritesDependencies
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies
import com.xwab.app.feature.sound.di.SoundDependencies
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.di.StoriesDependencies
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.navigation.NavigationState
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.savedIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import androidx.navigation3.runtime.get
import kotlinx.coroutines.flow.emptyFlow

class TabEntryProviderTest {
    @Test
    fun registeringAndResolvingAllFeaturesDoesNotInitializeTheirDependencies() {
        val provider = unopenedEntryProvider()
        val routes = listOf(BrowseRoute, FavoritesRoute, StoriesRoute, CategoryRoute("rain"), SoundRoute("rain"))

        // Inactive tab stacks also resolve entries. Their content has not been composed yet.
        val entries = routes.map(tabEntries(BrowseRoute, provider))

        assertEquals(routes.size, entries.size)
    }

    @Test
    fun theSameSoundInBrowseAndFavoritesHasIndependentDisplayIdentity() {
        val provider = unopenedEntryProvider()
        val state = NavigationState(
            startRoute = BrowseRoute,
            backStacks = mapOf(
                BrowseRoute to mutableListOf<NavKey>(BrowseRoute),
                FavoritesRoute to mutableListOf<NavKey>(FavoritesRoute),
            ),
        )
        val navigator = Navigator(state)
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
        val firstProvider = unopenedEntryProvider()
        val restoredProvider = unopenedEntryProvider()
        val first = tabEntries(FavoritesRoute, firstProvider)(SoundRoute("rain"))
        val restored = tabEntries(FavoritesRoute, restoredProvider)(SoundRoute("rain"))

        assertIs<String>(restored.contentKey)
        assertEquals(first.contentKey, restored.contentKey)
    }

    /** Saved UI state is keyed by the wire format, so renaming a route's class does not lose it. */
    @Test
    fun contentKeysFollowTheSavedNameRatherThanTheClassName() {
        val entry = tabEntries(FavoritesRoute, unopenedEntryProvider())(SoundRoute("rain"))

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

private fun unopenedEntryProvider() = appEntryProvider(
    UnopenedFeaturesGraph, onNavigate = {}, onBack = {}, onReselect = { emptyFlow() },
)

/** Fail immediately if entry registration or lookup eagerly opens any feature. */
private object UnopenedFeaturesGraph : AppGraph {
    override val browseDependencies: () -> BrowseDependencies = { error("Browse initialized before rendering") }
    override val favoritesDependencies: () -> FavoritesDependencies =
        { error("Favorites initialized before rendering") }
    override val categoryDependencies: () -> CategoryDependencies = { error("Category initialized before rendering") }
    override val soundDependencies: () -> SoundDependencies = { error("Sound initialized before rendering") }
    override val storiesDependencies: () -> StoriesDependencies = { error("Stories initialized before rendering") }

    /** The persistent now-playing bar resolves its port only when rendered. */
    override val nowPlayingDependencies: () -> NowPlayingDependencies =
        { error("Now playing initialized by navigation") }
}

/** The production metadata; these tests are about identity, not what the back arrow does. */
private fun tabEntries(tab: NavKey, provider: (NavKey) -> NavEntry<NavKey>) =
    entryProviderForTab(tab, backStack = emptyList(), provider, ::appEntryMetadata)
