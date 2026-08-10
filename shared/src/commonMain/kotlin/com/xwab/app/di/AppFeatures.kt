package com.xwab.app.di

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.category.categoryFeature
import com.xwab.app.feature.home.homeFeature
import com.xwab.app.feature.sounds.soundsFeature
import com.xwab.app.feature.story.storyFeature
import kotlinx.serialization.modules.SerializersModule

/**
 * Every feature the app ships.
 *
 * This list is the single place a new feature is registered: its Koin bindings reach [appModules],
 * its screens reach [appEntryProvider] and its route serializers reach [featureSerializers] from
 * here, so none of the three has to be edited again.
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
 * Every feature's slice of the navigation graph, folded into the one provider `NavDisplay` reads.
 *
 * A function rather than a value because each feature is handed the [navigator] it routes with, and
 * that only exists once the shell has composed. Order decides nothing — a route belongs to exactly
 * one feature — so this keeps the feature list's order rather than the bar's.
 *
 * `AppShell` calls this and nothing else, which is how that file still names no route. It lives
 * here rather than there so `AppModulesTest` can drive the real fold instead of a copy of it: a
 * route no feature claims throws on navigation, and the test turns that into a build failure.
 */
internal fun appEntryProvider(navigator: Navigator): (NavKey) -> NavEntry<NavKey> = entryProvider {
    val scope = this
    features.forEach { feature ->
        val contribute = feature.entries
        scope.contribute(navigator)
    }
}

/**
 * The polymorphic `NavKey` serializers of every feature, merged.
 *
 * A missing entry does not fail to compile — it fails when the back stack is restored after
 * process death, which is why collecting them is not left to hand.
 */
internal val featureSerializers: SerializersModule = SerializersModule {
    features.forEach { include(it.serializers) }
}
