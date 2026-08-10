package com.xwab.app.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.Module

/**
 * Everything the composition root needs to know about one feature.
 *
 * A feature publishes exactly one of these. Registering it is a single line in `shared`, instead
 * of a Koin module here and a serializers include there — two places that used to be edited
 * separately, and only one of which crashed the app when it was forgotten.
 *
 * @param koinModule the feature's bindings — its use cases and ViewModels. Objects only: what the
 *   feature *shows* is [entries], which is not a binding and does not belong in a container.
 * @param entries the feature's slice of the navigation graph — one `entry<Route> { }` per screen it
 *   owns. The [Navigator] arrives as a parameter rather than out of a composition local, so the
 *   wiring of a screen's callbacks is visible at the entry itself and a screen can be routed
 *   somewhere else without the shell being involved. Registration used to live in [koinModule],
 *   where it mixed the object graph with the navigation graph and hid the navigator inside a
 *   `LocalNavigator.current` that only worked because the shell happened to provide it.
 * @param serializers the polymorphic `NavKey` serializers for the feature's routes. Without them
 *   the back stack cannot be restored after process death.
 * @param topLevel the feature's place in the navigation bar, or null when it is only ever navigated
 *   into. This is what lets a new slice appear on screen without a line changing anywhere but the
 *   composition root's feature list: the shell builds the bar from whatever the features declare,
 *   instead of naming them itself.
 */
data class FeatureEntry(
    val koinModule: Module,
    val entries: EntryProviderScope<NavKey>.(Navigator) -> Unit,
    val serializers: SerializersModule = EmptySerializersModule(),
    val topLevel: TopLevelDestination? = null,
)
