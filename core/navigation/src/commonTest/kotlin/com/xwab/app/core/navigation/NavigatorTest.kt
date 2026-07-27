package com.xwab.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    @Test
    fun goBackKeepsTheRootEntry() {
        val backStack = mutableListOf<NavKey>(RootRoute)

        Navigator(backStack).goBack()

        assertEquals(1, backStack.size)
        assertEquals(RootRoute, backStack.first())
    }

    @Test
    fun goBackRemovesTheCurrentEntry() {
        val backStack = mutableListOf<NavKey>(RootRoute, DetailRoute)

        Navigator(backStack).goBack()

        assertEquals(1, backStack.size)
        assertEquals(RootRoute, backStack.first())
    }

    private data object RootRoute : NavKey
    private data object DetailRoute : NavKey
}
