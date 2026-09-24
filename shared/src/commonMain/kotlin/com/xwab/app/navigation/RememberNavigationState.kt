package com.xwab.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer

/**
 * Builds and restores the app's [NavigationState]: one back stack per tab, and the selected tab.
 *
 * This is the whole of the navigation setup, so the app shell holds none of it. Feature entry
 * wiring stays in the application composition root.
 */
@Composable
internal fun rememberNavigationState(): NavigationState {
    val destinations = TOP_LEVEL_DESTINATIONS
    val configuration = remember { SavedStateConfiguration { serializersModule = FEATURE_SERIALIZERS } }

    // One per tab, each rooted at its own destination. `key` gives each loop iteration a stable
    // Compose slot instead of having every remembered stack share the same position.
    val backStacks: Map<NavKey, NavBackStack<NavKey>> = destinations.associate { destination ->
        destination.route to key(destination.route) {
            rememberNavBackStack(configuration, destination.route)
        }
    }

    // The single source of truth for the selected tab: handed straight to `NavigationState`
    // rather than mirrored into it through a callback, so there is only ever one place it lives.
    //
    // It is persisted the way the Navigation 3 documentation persists it — as the route itself,
    // through the same `NavKey` polymorphism the back stacks already use — rather than as a
    // position in `TOP_LEVEL_DESTINATIONS`, which would restore the wrong tab once that list is
    // reordered. `navigation3-runtime` 1.1.x publishes its `NavKeySerializer` for Android only, not
    // for common code, so the polymorphic serializer the back stacks are saved with is named here.
    val selectedRoute = rememberSerializable(
        stateSerializer = PolymorphicSerializer(NavKey::class),
        configuration = configuration,
    ) {
        mutableStateOf(destinations.first().route)
    }

    return remember {
        // A feature dropped between releases leaves its route in every saved stack that held it.
        // FEATURE_SERIALIZERS reads those back as RetiredRoute rather than failing the restore;
        // this is where they are thrown away.
        dropRetiredRoutes(backStacks)

        // A tab dropped between releases restores a route that no longer has a stack of its own —
        // including RetiredRoute, which is what the selected tab reads back as once its feature is
        // gone. Fall back to the start tab rather than failing the whole restore on it.
        if (selectedRoute.value !in backStacks) {
            selectedRoute.value = destinations.first().route
        }
        NavigationState(
            startRoute = destinations.first().route,
            backStacks = backStacks,
            topLevelRouteState = selectedRoute,
        )
    }
}
