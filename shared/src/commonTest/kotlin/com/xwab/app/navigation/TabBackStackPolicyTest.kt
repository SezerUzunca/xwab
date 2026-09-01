package com.xwab.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The tab rules, driven with two fake tabs instead of the app's real three.
 *
 * Pushing within a tab is Decompose's own `pushToFront`/`pop` extensions now, not this policy's
 * concern — what stays app-specific, and easy to get subtly wrong, is which tab responds to a
 * reselect or a back press, and that a tab's root is never popped out from under `Children`.
 */
class TabBackStackPolicyTest {
    private enum class Tab { HOME, STORIES }

    private class FakeStacks(startDepth: Int = 1) {
        val depths = mutableMapOf(Tab.HOME to startDepth, Tab.STORIES to startDepth)
        val popped = mutableListOf<Tab>()

        fun policy() = TabBackStackPolicy(
            startTab = Tab.HOME,
            stackSize = { depths.getValue(it) },
            pop = { tab -> popped += tab; depths[tab] = depths.getValue(tab) - 1 },
            clearSubStack = { tab -> depths[tab] = 1 },
        )
    }

    @Test
    fun navigatingToAnotherTopLevelTabSwitchesInsteadOfPushing() {
        val fakes = FakeStacks()
        val policy = fakes.policy()

        policy.selectTab(Tab.STORIES)

        assertEquals(Tab.STORIES, policy.selectedTab.value)
        assertEquals(1, fakes.depths.getValue(Tab.HOME))
        assertFalse(Tab.HOME in fakes.popped)
    }

    @Test
    fun reselectingTheCurrentTabClearsItsSubStack() {
        val fakes = FakeStacks(startDepth = 1).apply { depths[Tab.HOME] = 3 }
        val policy = fakes.policy()

        policy.selectTab(Tab.HOME)

        assertEquals(Tab.HOME, policy.selectedTab.value)
        assertEquals(1, fakes.depths.getValue(Tab.HOME))
    }

    @Test
    fun reselectingAnotherTabDoesNotClearIt() {
        val fakes = FakeStacks().apply { depths[Tab.STORIES] = 3 }
        val policy = fakes.policy()
        policy.selectTab(Tab.STORIES)

        policy.selectTab(Tab.HOME)
        policy.selectTab(Tab.STORIES)

        assertEquals(3, fakes.depths.getValue(Tab.STORIES))
    }

    @Test
    fun goBackPopsTheSelectedTab() {
        val fakes = FakeStacks().apply { depths[Tab.HOME] = 2 }
        val policy = fakes.policy()

        policy.goBack()

        assertEquals(listOf(Tab.HOME), fakes.popped)
        assertEquals(1, fakes.depths.getValue(Tab.HOME))
    }

    /** An emptied stack has nothing for `Children` to render: the tab would vanish, not reset. */
    @Test
    fun goBackKeepsTheStartTabsRoot() {
        val fakes = FakeStacks()
        val policy = fakes.policy()

        policy.goBack()

        assertEquals(Tab.HOME, policy.selectedTab.value)
        assertEquals(emptyList(), fakes.popped)
        assertEquals(1, fakes.depths.getValue(Tab.HOME))
    }

    @Test
    fun goBackFromAnotherTabsRootFallsThroughToTheStartTab() {
        val fakes = FakeStacks()
        val policy = fakes.policy()
        policy.selectTab(Tab.STORIES)

        policy.goBack()

        assertEquals(Tab.HOME, policy.selectedTab.value)
        assertEquals(emptyList(), fakes.popped)
        assertEquals(1, fakes.depths.getValue(Tab.STORIES))
    }
}
