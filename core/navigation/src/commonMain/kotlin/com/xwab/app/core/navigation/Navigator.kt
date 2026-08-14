package com.xwab.app.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * The one thing a screen may do to navigation: ask for a key, or ask to go back.
 *
 * Which of the three things [navigate] means — reselect, switch tab, or push — is decided here
 * rather than by the caller, because nothing outside this module has to know whether a key happens
 * to be a tab. Features never see this class at all: their entries take plain callbacks, and the
 * composition root is the only caller.
 */
class Navigator(private val state: NavigationState) {
    /**
     * Resets the current tab when it is reselected, switches to another top-level route, or moves
     * a non-top-level route to the end of the current tab's stack.
     *
     * A tab is never pushed onto a stack. Doing that would put Stories on top of Sounds' history,
     * and backing out of it would land in the middle of the other tab.
     */
    fun navigate(key: NavKey) {
        when (key) {
            state.topLevelRoute -> clearSubStack()
            in state.backStacks -> state.topLevelRoute = key
            else -> state.currentBackStack.apply {
                remove(key)
                add(key)
            }
        }
    }

    /** Clears every entry above the current tab's root. */
    private fun clearSubStack() {
        state.currentBackStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }

    /**
     * Pops within the tab, and falls through to the start tab once there is nothing left to pop.
     *
     * A tab's root is never popped: a back stack that empties has nothing for `NavDisplay` to
     * render, and the tab would be gone rather than reset. From the start tab's own root this does
     * nothing at all, and the platform takes the press as leaving the app.
     */
    fun goBack() {
        val stack = state.currentBackStack
        when {
            stack.size > 1 -> stack.removeLastOrNull()
            state.topLevelRoute != state.startRoute -> state.topLevelRoute = state.startRoute
        }
    }
}
