package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

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
        startRoute = HomeRoute,
        backStacks = mapOf(
            HomeRoute to mutableListOf<NavKey>(HomeRoute),
            StoriesRoute to mutableListOf<NavKey>(StoriesRoute),
        ),
    )

    @Test
    fun navigatingToANonTopLevelRouteStaysInTheCurrentTab() {
        val state = state()

        Navigator(state).navigate(DetailRoute)

        assertEquals(HomeRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DetailRoute), state.backStacks.getValue(HomeRoute))
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    /** A tab pushed onto another tab's history is how backing out lands mid-way through it. */
    @Test
    fun navigatingToATopLevelRouteSwitchesTabsInsteadOfPushing() {
        val state = state()

        Navigator(state).navigate(StoriesRoute)

        assertEquals(StoriesRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(HomeRoute), state.backStacks.getValue(HomeRoute))
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    @Test
    fun reselectingTheCurrentTopLevelRouteClearsItsSubStack() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)

        navigator.selectTab(HomeRoute)

        assertEquals(HomeRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    @Test
    fun navigatingToAnExistingNonTopLevelRoutePopsToItWithoutReorderingParents() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)
        navigator.navigate(AnotherDetailRoute)

        navigator.navigate(DetailRoute)

        assertEquals(
            listOf<NavKey>(HomeRoute, DetailRoute),
            state.currentBackStack,
        )
    }

    @Test
    fun eachTabKeepsItsOwnHistoryAcrossASwitch() {
        val state = state()
        val navigator = Navigator(state)

        navigator.navigate(DetailRoute)
        navigator.navigate(StoriesRoute)
        navigator.navigate(HomeRoute)

        assertEquals(HomeRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DetailRoute), state.currentBackStack)
    }

    @Test
    fun goBackRemovesTheCurrentEntry() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)

        navigator.goBack()

        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    /** An emptied stack has nothing for `NavDisplay` to render: the tab would vanish, not reset. */
    @Test
    fun goBackKeepsTheStartTabsRoot() {
        val state = state()

        Navigator(state).goBack()

        assertEquals(HomeRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    @Test
    fun goBackFromAnotherTabsRootFallsThroughToTheStartTab() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(StoriesRoute)

        navigator.goBack()

        assertEquals(HomeRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    /** Back from a tab lands on the start tab, so the start tab is always on screen beneath it. */
    @Test
    fun theStartTabStaysInUseWhileAnotherTabIsShowing() {
        val state = state()
        assertEquals(listOf<NavKey>(HomeRoute), state.routesInUse)

        Navigator(state).navigate(StoriesRoute)

        assertEquals(listOf<NavKey>(HomeRoute, StoriesRoute), state.routesInUse)
    }

    @Test
    fun aStartRouteWithNoBackStackIsRejectedRatherThanFailingOnTheFirstBackPress() {
        assertFailsWith<IllegalArgumentException> {
            NavigationState(
                startRoute = HomeRoute,
                backStacks = mapOf(StoriesRoute to mutableListOf<NavKey>(StoriesRoute)),
            )
        }
    }

    @Test
    fun aProgrammaticTabIntentDoesNotClearTheSelectedTabsHistory() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)
        navigator.navigate(HomeRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DetailRoute), state.currentBackStack)
    }

    @Test
    fun rapidRepeatedNavigationCannotCreateDuplicateContentKeys() {
        val state = state()
        val navigator = Navigator(state)
        repeat(10) { navigator.navigate(DetailRoute) }
        assertEquals(listOf<NavKey>(HomeRoute, DetailRoute), state.currentBackStack)
    }

    @Test
    fun replacingATransientEntryWithAnExistingDestinationPopsToThatDestination() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)
        navigator.navigate(AnotherDetailRoute)
        navigator.replaceCurrent(DetailRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DetailRoute), state.currentBackStack)
        navigator.goBack()
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    @Test
    fun rootReselectionEmitsOnlyToThatTabsList() = runBlocking {
        val state = state()
        val navigator = Navigator(state)
        val event = async(start = CoroutineStart.UNDISPATCHED) { navigator.reselections(HomeRoute).first() }
        navigator.selectTab(HomeRoute)
        assertEquals(Unit, event.await())
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    @Test
    fun selectingANonTabIsRejectedWithoutMutatingTheStack() {
        val state = state()
        assertFailsWith<IllegalArgumentException> { Navigator(state).selectTab(DetailRoute) }
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    /**
     * With a category and its sound side by side, the arrow on the category pane means the
     * category. Back would close the sound — the latest entry — instead.
     */
    @Test
    fun upFromAMiddlePaneClosesThatPaneAndWhatWasOpenedFromIt() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)
        navigator.navigate(AnotherDetailRoute)

        navigator.goUp(DetailRoute)

        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    @Test
    fun upFromTheLatestPaneIsBackAndAnUnknownOrRootDestinationFallsBackToIt() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(DetailRoute)
        navigator.navigate(AnotherDetailRoute)

        navigator.goUp(AnotherDetailRoute)
        assertEquals(listOf<NavKey>(HomeRoute, DetailRoute), state.currentBackStack)

        // Not in this stack: nothing better to close than the latest entry.
        navigator.goUp(StoriesRoute)
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)

        // The root is never removed; from another tab's root this falls through like Back.
        navigator.navigate(StoriesRoute)
        navigator.goUp(StoriesRoute)
        assertEquals(HomeRoute, state.topLevelRoute)
        assertEquals(listOf<NavKey>(StoriesRoute), state.backStacks.getValue(StoriesRoute))
    }

    /**
     * Choosing Ocean after Rain is a new selection, not a step deeper: beside a list it replaces the
     * detail pane, and Back leaves the selection instead of walking through every earlier one.
     */
    @Test
    fun openingAnotherDestinationOfTheSameKindReplacesItAndWhatItOpened() {
        val state = state()
        val navigator = Navigator(state)
        navigator.navigate(ItemRoute("rain"))
        navigator.navigate(DetailRoute)

        navigator.navigate(ItemRoute("ocean"))

        assertEquals(listOf<NavKey>(HomeRoute, ItemRoute("ocean")), state.currentBackStack)
        navigator.goBack()
        assertEquals(listOf<NavKey>(HomeRoute), state.currentBackStack)
    }

    @Test
    fun invalidRootEmptyAndDuplicateStacksAreRejected() {
        for (stack in listOf(emptyList(), listOf(DetailRoute), listOf(HomeRoute, DetailRoute, DetailRoute))) {
            assertFailsWith<IllegalArgumentException> {
                NavigationState(HomeRoute, mapOf(HomeRoute to stack.toMutableList()))
            }
        }
    }

    private data object HomeRoute : NavKey
    private data object StoriesRoute : NavKey
    private data object DetailRoute : NavKey
    private data object AnotherDetailRoute : NavKey
    private data class ItemRoute(val id: String) : NavKey
}
