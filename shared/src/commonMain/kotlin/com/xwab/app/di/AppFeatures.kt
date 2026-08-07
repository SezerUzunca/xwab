package com.xwab.app.di

import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.category.categoryFeature
import com.xwab.app.feature.home.homeFeature
import com.xwab.app.feature.sounds.soundsFeature
import com.xwab.app.feature.story.storyFeature
import kotlinx.serialization.modules.SerializersModule

/**
 * Every feature the app ships.
 *
 * This list is the single place a new feature is registered: its Koin bindings reach
 * [appModules] and its route serializers reach [featureSerializers] from here, so neither has to
 * be edited again.
 */
internal val features: List<FeatureEntry> = listOf(
    homeFeature,
    categoryFeature,
    soundsFeature,
    storyFeature,
)

/**
 * The navigation bar, in the order the features asked for.
 *
 * The first one is the start destination: the tab back falls through to and the app exits from.
 * `AppShell` reads this and nothing else — it names no feature, so a new slice reaches the bar by
 * appearing in [features] above with a `topLevel` on its entry.
 */
internal val topLevelDestinations: List<TopLevelDestination> =
    features.mapNotNull { it.topLevel }.sortedBy { it.order }

/**
 * The polymorphic `NavKey` serializers of every feature, merged.
 *
 * A missing entry does not fail to compile — it fails when the back stack is restored after
 * process death, which is why collecting them is not left to hand.
 */
internal val featureSerializers: SerializersModule = SerializersModule {
    features.forEach { include(it.serializers) }
}
