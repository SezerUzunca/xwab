package com.xwab.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The tab rules, driven without a composition.
 *
 * Every one of these used to be a property of a single back stack the shell owned, which meant
 * none of them could be checked outside a running app. [NavigationState] holds the same state as
 * plain lists, so the rules that are easy to get subtly wrong — a tab pushed onto another tab's
 * history, a root popped out from under `NavDisplay` — are checked from both sides here.
 */
class NavigatorTest {
    private fun state(): NavigationState = NavigationState(
        startRoute = SoundsRoute,
        backStacks = mapOf(
            SoundsRoute to mutableListOf<NavKey>(SoundsRoute),
            StoriesRoute to mutableListOf<NavKey>(StoriesRoute),
        ),
    )

    @Test
    fun navigatingToANonTopLevelRouteStaysInTheCurrentTab() {
        val state = state()

        Navigator(state).navigate(DetailRoute)

        assertEquals(SoundsRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(SoundsRoute, DetailRoute), state.backStacks.getValue(SoundsRoute))
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    /** A tab pushed onto another tab's history is how backing out lands mid-way through it. */
    @Test
    fun navigatingToATopLevelRouteSwitchesTabsInsteadOfPushing() {
        val state = state()

        Navigator(state).navigate(StoriesRoute)

        assertEquals(StoriesRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(SoundsRoute), state.backStacks.getValue(SoundsRoute))
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    @Test
    fun eachTabKeepsItsOwnHistoryAcrossASwitch() {
        val state = state()
        val navigator = Navigator(state)

        navigator.navigate(DetailRoute)
        navigator.navigate(StoriesRoute)
        navigator.navigate(SoundsRoute)

        assertEquals(SoundsRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(SoundsRoute, DetailRoute), state.currentBackStack)
    }

    @Test
    fun goBackRemovesTheCurrentEntry() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)

        navigator.goBack()

        assertEquals(listOf<NavKey>(SoundsRoute), state.currentBackStack)
    }

    /** An emptied stack has nothing for `NavDisplay` to render: the tab would vanish, not reset. */
    @Test
    fun goBackKeepsTheStartTabsRoot() {
        val state = state()

        Navigator(state).goBack()

        assertEquals(SoundsRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(SoundsRoute), state.currentBackStack)
    }

    @Test
    fun goBackFromAnotherTabsRootFallsThroughToTheStartTab() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(StoriesRoute)

        navigator.goBack()

        assertEquals(SoundsRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    /** Back from a tab lands on the start tab, so the start tab is always on screen beneath it. */
    @Test
    fun theStartTabStaysInUseWhileAnotherTabIsShowing() {
        val state = state()
        assertEquals(listOf<NavKey>(SoundsRoute), state.routesInUse)

        Navigator(state).navigate(StoriesRoute)

        assertEquals(listOf<NavKey>(SoundsRoute, StoriesRoute), state.routesInUse)
    }

    @Test
    fun aStartRouteWithNoBackStackIsRejectedRatherThanFailingOnTheFirstBackPress() {
        assertFailsWith<IllegalArgumentException> {
            NavigationState(
                startRoute = SoundsRoute,
                backStacks = mapOf(StoriesRoute to mutableListOf<NavKey>(StoriesRoute)),
            )
        }
    }

    private data object SoundsRoute : NavKey
    private data object StoriesRoute : NavKey
    private data object DetailRoute : NavKey
}
