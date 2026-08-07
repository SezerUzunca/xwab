package com.xwab.app.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey

/**
 * One back stack per tab, and which tab is showing.
 *
 * A single back stack cannot do this: switching from Stories back to Sounds has to find Sounds
 * exactly where it was left, three screens deep if that is where the listener stopped. So each
 * top-level destination keeps its own history and the shell renders the one that is selected.
 *
 * Deliberately free of Compose *composition* — it holds a [MutableState] so the shell recomposes,
 * but nothing here is `@Composable`. [NavigatorTest] drives the whole thing without a UI, which is
 * the only way the tab rules below are checked from both sides.
 *
 * Follows the multiple-back-stacks recipe in the Navigation 3 documentation, with one deviation:
 * the selected tab is persisted by the shell as an index rather than through a `NavKey` serializer,
 * because `navigation3-runtime` 1.1.1 publishes no such serializer for Kotlin Multiplatform.
 *
 * @param startRoute the tab back falls through to, and the one the app exits from.
 * @param backStacks one stack per top-level route, each already holding that route as its root.
 */
class NavigationState(
    val startRoute: NavKey,
    val backStacks: Map<NavKey, MutableList<NavKey>>,
    topLevelRoute: MutableState<NavKey> = mutableStateOf(startRoute),
) {
    init {
        require(startRoute in backStacks) {
            "The start route must have a back stack of its own: $startRoute"
        }
    }

    /** The tab currently showing. Set by [Navigator]; read by the navigation bar. */
    var topLevelRoute: NavKey by topLevelRoute

    val currentBackStack: MutableList<NavKey>
        get() = backStacks.getValue(topLevelRoute)

    /**
     * The stacks whose entries are on screen — the selected tab's, and the start tab's beneath it.
     *
     * The start tab stays in the list so that back from another tab's root lands on it rather than
     * leaving the app. That is the "exit through home" behaviour the documented recipe describes,
     * and it is why this is a list rather than a single stack.
     */
    val routesInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}
