package com.xwab.app.composition

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.navigation.appNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Where each feature's intents lead, checked on the real navigator's stacks.
 *
 * The callbacks are the one part of entry wiring the shell still writes by hand, so a swapped
 * destination or a dropped id would compile and pass every feature's own tests.
 */
class AppEntryCallbacksTest {
    private val state = appNavigationState()
    private val graph = appEntryGraph(state)
    private val navigator = graph.navigator

    /**
     * The graph builds one navigator, not one per request.
     *
     * The host reads `graph.navigator` and every callback provider is injected with the same
     * binding. What the callbacks then do with it is checked below, and end to end by the integration
     * scenario. Reselection delivery is checked through the result decorator in composition tests.
     */
    @Test
    fun theGraphBuildsOneNavigator() {
        assertSame(graph.navigator, graph.navigator)
    }

    @Test
    fun featureIntentsOpenTheExpectedDestinationsWithTheirIds() {
        graph.provideBrowseCallbacks(navigator).onCategoryClick(CategoryId("rain / yağmur:夜"))
        graph.provideCategoryCallbacks(navigator).onTrackClick(TrackId("rain|night/%25"))
        assertEquals(
            listOf(BrowseRoute, CategoryRoute("rain / yağmur:夜"), SoundRoute("rain|night/%25")),
            state.backStacks.getValue(BrowseRoute),
        )

        navigator.selectTab(FavoritesRoute)
        graph.provideFavoritesCallbacks(navigator).onTrackClick(TrackId("ocean"))
        assertEquals(listOf(FavoritesRoute, SoundRoute("ocean")), state.backStacks.getValue(FavoritesRoute))

        graph.provideFavoritesCallbacks(navigator).onBrowse()
        assertEquals(BrowseRoute, state.topLevelRoute)

        navigator.selectTab(StoriesRoute)
        graph.provideStoriesCallbacks(navigator).onStoryClick(StoryId("forest"))
        assertEquals(listOf(StoriesRoute, StoryRoute("forest")), state.backStacks.getValue(StoriesRoute))
    }

    @Test
    fun detailBackActionsCloseTheirOwnScreen() {
        navigator.navigate(CategoryRoute("rain"))
        navigator.navigate(SoundRoute("ocean"))

        graph.provideSoundCallbacks(navigator).onBack()
        assertEquals(listOf(BrowseRoute, CategoryRoute("rain")), state.currentBackStack)
        graph.provideCategoryCallbacks(navigator).onBack()
        assertEquals(listOf<NavKey>(BrowseRoute), state.currentBackStack)

        navigator.selectTab(StoriesRoute)
        navigator.navigate(StoryRoute("forest"))
        graph.provideStoriesCallbacks(navigator).onBack()
        assertEquals(listOf<NavKey>(StoriesRoute), state.currentBackStack)
    }
}
