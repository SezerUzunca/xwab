package com.xwab.app.navigation

/** The app's real tabs, each stack at its root and the start tab selected, as on a fresh launch. */
internal fun appNavigationState(): NavigationState = NavigationState(
    startRoute = TOP_LEVEL_DESTINATIONS.first().route,
    backStacks = TOP_LEVEL_DESTINATIONS.associate { it.route to mutableListOf(it.route) },
)
