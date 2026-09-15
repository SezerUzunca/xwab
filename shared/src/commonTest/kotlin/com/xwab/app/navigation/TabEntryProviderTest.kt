package com.xwab.app.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.browse.di.BrowseDependencies
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.di.CategoryDependencies
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.di.FavoritesDependencies
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.di.SoundDependencies
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.di.StoriesDependencies
import com.xwab.app.feature.story.navigation.StoriesRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class TabEntryProviderTest {
    @Test
    fun registeringAndResolvingAllFeaturesDoesNotInitializeTheirDependencies() {
        val provider = appEntryProvider(UnopenedFeaturesGraph, onNavigate = {}, onBack = {})
        val routes = listOf(BrowseRoute, FavoritesRoute, StoriesRoute, CategoryRoute("rain"), SoundRoute("rain"))

        // Inactive tab stacks also resolve entries. Their content has not been composed yet.
        val entries = routes.map(entryProviderForTab(BrowseRoute, provider))

        assertEquals(routes.size, entries.size)
    }

    @Test
    fun theSameSoundInBrowseAndFavoritesHasIndependentDisplayIdentity() {
        val provider = appEntryProvider(UnopenedFeaturesGraph, onNavigate = {}, onBack = {})
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
            state.backStacks.getValue(tab).map(entryProviderForTab(tab, provider))
        }

        assertEquals(5, entries.size)
        assertEquals(entries.size, entries.map { it.contentKey }.toSet().size)
        assertNotEquals(entries[2].contentKey, entries[4].contentKey)
    }

    @Test
    fun recreatingProvidersAndRoutesKeepsTheSameSaveableIdentity() {
        val firstProvider = appEntryProvider(UnopenedFeaturesGraph, onNavigate = {}, onBack = {})
        val restoredProvider = appEntryProvider(UnopenedFeaturesGraph, onNavigate = {}, onBack = {})
        val first = entryProviderForTab(FavoritesRoute, firstProvider)(SoundRoute("rain"))
        val restored = entryProviderForTab(FavoritesRoute, restoredProvider)(SoundRoute("rain"))

        assertIs<String>(restored.contentKey)
        assertEquals(first.contentKey, restored.contentKey)
    }

    @Test
    fun tabIdentityPreservesFeatureMetadataAndCustomContentKeys() {
        val metadata = mapOf<String, Any>("custom-scene" to true)
        fun provider(contentKey: String): (NavKey) -> NavEntry<NavKey> = { key ->
            NavEntry(key = key, contentKey = contentKey, metadata = metadata) {}
        }

        val first = entryProviderForTab(BrowseRoute, provider("first"))(SoundRoute("rain"))
        val second = entryProviderForTab(BrowseRoute, provider("second"))(SoundRoute("rain"))

        assertSame(metadata, first.metadata)
        assertNotEquals(first.contentKey, second.contentKey)
    }
}

/** Fail immediately if entry registration or lookup eagerly opens any feature. */
private object UnopenedFeaturesGraph : AppGraph {
    override val browseDependencies: () -> BrowseDependencies = { error("Browse initialized before rendering") }
    override val favoritesDependencies: () -> FavoritesDependencies = { error("Favorites initialized before rendering") }
    override val categoryDependencies: () -> CategoryDependencies = { error("Category initialized before rendering") }
    override val soundDependencies: () -> SoundDependencies = { error("Sound initialized before rendering") }
    override val storiesDependencies: () -> StoriesDependencies = { error("Stories initialized before rendering") }
}
