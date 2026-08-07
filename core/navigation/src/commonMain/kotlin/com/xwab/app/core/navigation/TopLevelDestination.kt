package com.xwab.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * A feature's claim to a place in the app's navigation bar.
 *
 * A feature that publishes one of these becomes a tab; a feature that leaves it null is reachable
 * only by navigating into it from somewhere else. `feature:sounds` and `feature:category` are the
 * second kind — a listener drills into them, they are not places to switch to.
 *
 * [label] and [icon] are slots rather than a `StringResource` and an `ImageVector` on purpose. Each
 * feature owns its own Compose resources and this module cannot see them, and taking an
 * `ImageVector` would put the design system on the classpath of every navigation module. As slots,
 * the feature fills both in its own module and this one stays a plain registry.
 *
 * @param route the tab's own root. It is the first entry of that tab's back stack and never popped.
 * @param order where the tab sits in the bar. The lowest is the start destination: the tab back
 *   returns to, and the one the app exits from.
 */
class TopLevelDestination(
    val route: NavKey,
    val order: Int,
    val label: @Composable () -> String,
    val icon: @Composable () -> Unit,
)
