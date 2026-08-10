package com.xwab.app.di

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.xwab.app.core.navigation.FeatureEntry
import com.xwab.app.core.navigation.Navigator
import com.xwab.app.core.navigation.TopLevelDestination
import com.xwab.app.feature.category.categoryFeature
import com.xwab.app.feature.sounds.soundsFeature
import com.xwab.app.feature.story.storyFeature
import com.xwab.app.home.homeEntry
import com.xwab.app.home.homeNavigationSerializers
import com.xwab.app.home.homeTopLevel
import kotlinx.serialization.modules.SerializersModule

/**
 * Every feature the app ships.
 *
 * This list is the single place a new feature is registered: its Koin bindings reach [appModules],
 * its screens reach [appEntryProvider] and its route serializers reach [featureSerializers] from
 * here, so none of the three has to be edited again.
 *
 * Home is deliberately absent. It is not a slice — it lives in this module, under
 * `com.xwab.app.home`, and each of the three collectors below names it directly. That is the shape
 * Now in Android uses, where `NiaApp` lists `forYouEntry` and `TopLevelNavItem` lists
 * `ForYouNavKey`. It costs what `com.xwab.app.home.homeEntry`'s KDoc spells out: the architecture
 * rules cannot see a screen that lives here.
 */
internal val features: List<FeatureEntry> = listOf(
    categoryFeature,
    soundsFeature,
    storyFeature,
)

/**
 * The navigation bar: home first, then whatever the features asked for.
 *
 * The lowest order is the start destination — the tab back falls through to, and the one the app
 * exits from. `AppShell` reads this and nothing else, so it still names no route of its own.
 */
internal val topLevelDestinations: List<TopLevelDestination> =
    (listOf(homeTopLevel) + features.mapNotNull { it.topLevel }).sortedBy { it.order }

/**
 * The whole navigation graph, folded into the one provider `NavDisplay` reads.
 *
 * A function rather than a value because home and each feature are handed the [navigator] they
 * route with, and that only exists once the shell has composed. Order decides nothing — a route
 * belongs to exactly one registrant.
 *
 * `AppShell` calls this and nothing else. It lives here rather than there so `AppModulesTest` can
 * drive the real fold instead of a copy of it: a route nothing claims throws on navigation, and the
 * test turns that into a build failure.
 */
internal fun appEntryProvider(navigator: Navigator): (NavKey) -> NavEntry<NavKey> = entryProvider {
    val scope = this
    scope.homeEntry(navigator)
    features.forEach { feature ->
        val contribute = feature.entries
        scope.contribute(navigator)
    }
}

/**
 * The polymorphic `NavKey` serializers of home and every feature, merged.
 *
 * A missing entry does not fail to compile — it fails when the back stack is restored after
 * process death, which is why collecting them is not left to hand.
 */
internal val featureSerializers: SerializersModule = SerializersModule {
    include(homeNavigationSerializers)
    features.forEach { include(it.serializers) }
}
