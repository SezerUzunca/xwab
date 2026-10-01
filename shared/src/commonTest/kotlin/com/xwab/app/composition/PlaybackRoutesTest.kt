package com.xwab.app.composition

import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoryRoute
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.navigation.NavigationState
import com.xwab.app.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where the now-playing bar takes a listener.
 *
 * The bar hands over a [PlaybackItemId]; this module turns it into a tab and a route. Worth checking
 * from the outside because the two kinds are answered differently for a reason, and a mapping that
 * quietly sent a story to a sound screen would compile.
 */
class PlaybackRoutesTest {

    @Test
    fun aPlayingSoundOpensItsOwnScreenUnderSounds() {
        val opened = mutableListOf<Pair<NavKey, NavKey>>()

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "gentle-rain")) { tab, route -> opened += tab to route }

        assertEquals(listOf<Pair<NavKey, NavKey>>(BrowseRoute to SoundRoute("gentle-rain")), opened)
    }

    @Test
    fun aPlayingStoryOpensItsOwnDetailsUnderStories() {
        val opened = mutableListOf<Pair<NavKey, NavKey>>()

        openPlaybackDetails(PlaybackItemId(STORY_PLAYBACK_KIND, "moonlit-forest")) { tab, route ->
            opened += tab to route
        }

        assertEquals(listOf<Pair<NavKey, NavKey>>(StoriesRoute to StoryRoute("moonlit-forest")), opened)
    }

    /** The tab the listener was on keeps its stack; Back from the item returns to its own list. */
    @Test
    fun theBarOpensTheItemInItsOwnTabAndLeavesTheCurrentTabAlone() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(FavoritesRoute)
        navigator.navigate(SoundRoute("rain"))

        openPlaybackDetails(PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"), navigator::openInTab)

        assertEquals(StoriesRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(StoriesRoute, StoryRoute("bedtime")), state.currentBackStack)
        assertEquals(listOf<NavKey>(FavoritesRoute, SoundRoute("rain")), state.backStacks.getValue(FavoritesRoute))
        navigator.goBack()
        assertEquals(listOf<NavKey>(StoriesRoute), state.currentBackStack)
    }

    /**
     * The catalog case this guards: a sound from the bar never lands beside a category it may not
     * belong to, where it would take the extra pane beside the wrong list.
     */
    @Test
    fun aSoundFromTheBarLeavesACategoryItDoesNotBelongTo() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(CategoryRoute("rain"))

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "waves"), navigator::openInTab)

        assertEquals(listOf<NavKey>(BrowseRoute, SoundRoute("waves")), state.currentBackStack)
    }

    /** The item's own screen, already open in its tab, keeps the context it was opened in. */
    @Test
    fun anItemAlreadyOpenInItsTabIsReturnedTo() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(CategoryRoute("rain"))
        navigator.navigate(SoundRoute("rain"))
        navigator.navigate(FavoritesRoute)

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"), navigator::openInTab)

        assertEquals(BrowseRoute, state.topLevelRoute)
        assertEquals(
            listOf<NavKey>(BrowseRoute, CategoryRoute("rain"), SoundRoute("rain")),
            state.currentBackStack,
        )
    }

    @Test
    fun anUnknownContentKindOpensNothing() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(FavoritesRoute)

        openPlaybackDetails(PlaybackItemId("removed-kind", "item"), navigator::openInTab)

        assertEquals(FavoritesRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(FavoritesRoute), state.currentBackStack)
    }

    /** A detail is its item's own screen; the now-playing bar hides only for that same item. */
    @Test
    fun detailsNameTheItemTheyShowAndOtherScreensNameNone() {
        assertEquals(PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"), SoundRoute("rain").playbackItem())
        assertEquals(PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"), StoryRoute("bedtime").playbackItem())
        assertEquals(null, BrowseRoute.playbackItem())
        assertEquals(null, CategoryRoute("rain").playbackItem())
    }

    private fun navigationState() = NavigationState(
        startRoute = BrowseRoute,
        backStacks = mapOf(
            BrowseRoute to mutableListOf<NavKey>(BrowseRoute),
            FavoritesRoute to mutableListOf<NavKey>(FavoritesRoute),
            StoriesRoute to mutableListOf<NavKey>(StoriesRoute),
        ),
    )
}
