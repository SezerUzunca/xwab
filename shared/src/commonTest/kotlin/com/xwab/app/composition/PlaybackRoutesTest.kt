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
import com.xwab.app.feature.nowplaying.navigation.NowPlayingRoute
import com.xwab.app.navigation.NavigationState
import com.xwab.app.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Player entry navigation and the content-neutral details intent.
 *
 * The player screen hands over a [PlaybackItemId]; this module turns it into a destination route.
 * Worth checking from the outside because the two kinds are answered differently for a reason, and
 * a mapping that quietly sent a story to a sound screen would compile.
 */
class PlaybackRoutesTest {

    @Test
    fun aPlayingSoundOpensItsOwnScreen() {
        val opened = mutableListOf<NavKey>()

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "gentle-rain"), opened::add)

        assertEquals(listOf<NavKey>(SoundRoute("gentle-rain")), opened)
    }

    @Test
    fun aPlayingStoryOpensItsOwnDetails() {
        val opened = mutableListOf<NavKey>()

        openPlaybackDetails(PlaybackItemId(STORY_PLAYBACK_KIND, "moonlit-forest"), opened::add)

        assertEquals(listOf<NavKey>(StoryRoute("moonlit-forest")), opened)
    }

    @Test
    fun openingThePlayerKeepsTheSelectedTabAndBackReturnsToItsPreviousScreen() {
        val state = navigationState()
        val navigator = Navigator(state)
        val sound = SoundRoute("rain")
        navigator.navigate(FavoritesRoute)
        navigator.navigate(sound)

        navigator.navigate(NowPlayingRoute)
        navigator.navigate(NowPlayingRoute)

        assertEquals(FavoritesRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(FavoritesRoute, sound, NowPlayingRoute), state.currentBackStack)
        navigator.goBack()
        assertEquals(listOf<NavKey>(FavoritesRoute, sound), state.currentBackStack)
        assertEquals(listOf<NavKey>(BrowseRoute), state.backStacks.getValue(BrowseRoute))
    }

    @Test
    fun openingDetailsRemovesThePlayerSoBackCannotReopenIt() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(FavoritesRoute)
        navigator.navigate(NowPlayingRoute)

        openPlaybackDetails(
            PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"),
            navigator::replaceCurrent,
        )

        assertEquals(FavoritesRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(FavoritesRoute, StoryRoute("bedtime")), state.currentBackStack)
        navigator.goBack()
        assertEquals(listOf<NavKey>(FavoritesRoute), state.currentBackStack)
    }

    /** Details for another item are a fresh selection from the list, not a step past the open one. */
    @Test
    fun detailsForAnotherItemOpenFromTheTabsRoot() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(FavoritesRoute)
        navigator.navigate(SoundRoute("rain"))
        navigator.navigate(NowPlayingRoute)

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "waves"), navigator::replaceCurrent)

        assertEquals(listOf<NavKey>(FavoritesRoute, SoundRoute("waves")), state.currentBackStack)
    }

    /**
     * The catalog case this guards: a sound from the player never lands beside a category it may
     * not belong to, where it would take the extra pane beside the wrong list.
     */
    @Test
    fun aSoundFromThePlayerLeavesTheCategoryItWasOpenedAbove() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(CategoryRoute("rain"))
        navigator.navigate(NowPlayingRoute)

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "waves"), navigator::replaceCurrent)

        assertEquals(listOf<NavKey>(BrowseRoute, SoundRoute("waves")), state.currentBackStack)
    }

    /** The item's own detail, already open beneath the player, keeps the context it was opened in. */
    @Test
    fun detailsAlreadyOpenBeneathThePlayerAreReturnedTo() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(CategoryRoute("rain"))
        navigator.navigate(SoundRoute("rain"))
        navigator.navigate(NowPlayingRoute)

        openPlaybackDetails(PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"), navigator::replaceCurrent)

        assertEquals(
            listOf<NavKey>(BrowseRoute, CategoryRoute("rain"), SoundRoute("rain")),
            state.currentBackStack,
        )
    }

    @Test
    fun anUnknownContentKindDoesNotPopThePlayerOrNavigate() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(NowPlayingRoute)

        openPlaybackDetails(PlaybackItemId("removed-kind", "item"), navigator::replaceCurrent)

        assertEquals(listOf<NavKey>(BrowseRoute, NowPlayingRoute), state.currentBackStack)
    }

    /** A detail is its item's own screen; the mini player hides only for that same item. */
    @Test
    fun detailsNameTheItemTheyShowAndOtherScreensNameNone() {
        assertEquals(PlaybackItemId(SOUND_PLAYBACK_KIND, "rain"), SoundRoute("rain").playbackItem())
        assertEquals(PlaybackItemId(STORY_PLAYBACK_KIND, "bedtime"), StoryRoute("bedtime").playbackItem())
        assertEquals(null, BrowseRoute.playbackItem())
        assertEquals(null, NowPlayingRoute.playbackItem())
    }

    private fun navigationState() = NavigationState(
        startRoute = BrowseRoute,
        backStacks = mapOf(
            BrowseRoute to mutableListOf<NavKey>(BrowseRoute),
            FavoritesRoute to mutableListOf<NavKey>(FavoritesRoute),
        ),
    )
}
