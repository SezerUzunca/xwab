package com.xwab.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * The one thing a screen may do to navigation: ask for a key, or ask to go back.
 *
 * Which of the two things [navigate] means is decided here rather than by the caller, because a
 * screen has no business knowing whether what it is routing to happens to be a tab. A feature's
 * `navigateToX` extension calls [navigate] either way, and the tab-versus-push distinction stays in
 * this module.
 */
class Navigator(private val state: NavigationState) {
    /**
     * Switches tab when [key] is a top-level route, and pushes onto the current tab otherwise.
     *
     * A tab is never pushed onto a stack. Doing that would put Stories on top of Sounds' history,
     * and backing out of it would land in the middle of the other tab.
     */
    fun navigate(key: NavKey) {
        if (key in state.backStacks) {
            state.topLevelRoute = key
        } else {
            state.currentBackStack.add(key)
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

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator is not available")
}

@Composable
fun rememberNavigator(state: NavigationState): Navigator = remember(state) { Navigator(state) }
