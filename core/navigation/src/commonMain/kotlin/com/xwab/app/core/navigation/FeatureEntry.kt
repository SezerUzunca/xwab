package com.xwab.app.core.navigation

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
 * The screen composables themselves are not listed: a feature's Koin module declares them with
 * `navigation<Route> { }`, and `koinEntryProvider()` collects them from the container.
 *
 * @param koinModule the feature's bindings — its use cases, ViewModels and screen entries.
 * @param serializers the polymorphic `NavKey` serializers for the feature's routes. Without them
 *   the back stack cannot be restored after process death.
 */
data class FeatureEntry(
    val koinModule: Module,
    val serializers: SerializersModule = EmptySerializersModule(),
)
