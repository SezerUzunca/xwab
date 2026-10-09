package com.xwab.app.composition

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEventBus
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.appNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where each feature's intents lead, checked on the real navigator's stacks.
 *
 * The callbacks are the one part of entry wiring the shell still writes by hand, so a swapped
 * destination or a dropped id would compile and pass every feature's own tests. That the
 * composition root's entry graph hands these to the real entries is checked end to end by the
 * integration scenario.
 */
class AppEntryCallbacksTest {
    private val state = appNavigationState()
    private val navigator = Navigator(state, ResultEventBus())

    @Test
    fun featureIntentsOpenTheExpectedDestinationsWithTheirIds() {
        AppEntryCallbacks.provideBrowseCallbacks(navigator).onCategoryClick(CategoryId("rain / yağmur:夜"))
        AppEntryCallbacks.provideCategoryCallbacks(navigator).onTrackClick(TrackId("rain|night/%25"))
        assertEquals(
            listOf(BrowseRoute, CategoryRoute("rain / yağmur:夜"), SoundRoute("rain|night/%25")),
            state.backStacks.getValue(BrowseRoute),
        )

        navigator.selectTab(FavoritesRoute)
        AppEntryCallbacks.provideFavoritesCallbacks(navigator).onTrackClick(TrackId("ocean"))
        assertEquals(listOf(FavoritesRoute, SoundRoute("ocean")), state.backStacks.getValue(FavoritesRoute))

        AppEntryCallbacks.provideFavoritesCallbacks(navigator).onBrowse()
        assertEquals(BrowseRoute, state.topLevelRoute)

        navigator.selectTab(StoriesRoute)
        AppEntryCallbacks.provideStoriesCallbacks(navigator).onStoryClick(StoryId("forest"))
        assertEquals(listOf(StoriesRoute, StoryRoute("forest")), state.backStacks.getValue(StoriesRoute))
    }

    @Test
    fun detailBackActionsCloseTheirOwnScreen() {
        navigator.navigate(CategoryRoute("rain"))
        navigator.navigate(SoundRoute("ocean"))

        AppEntryCallbacks.provideSoundCallbacks(navigator).onBack()
        assertEquals(listOf(BrowseRoute, CategoryRoute("rain")), state.currentBackStack)
        AppEntryCallbacks.provideCategoryCallbacks(navigator).onBack()
        assertEquals(listOf<NavKey>(BrowseRoute), state.currentBackStack)

        navigator.selectTab(StoriesRoute)
        navigator.navigate(StoryRoute("forest"))
        AppEntryCallbacks.provideStoriesCallbacks(navigator).onBack()
        assertEquals(listOf<NavKey>(StoriesRoute), state.currentBackStack)
    }
}
