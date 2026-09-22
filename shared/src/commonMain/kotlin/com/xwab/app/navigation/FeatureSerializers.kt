package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.browse.navigation.browseNavigationSerializers
import com.xwab.app.feature.category.navigation.categoryNavigationSerializers
import com.xwab.app.feature.favorites.navigation.favoritesNavigationSerializers
import com.xwab.app.feature.sound.navigation.soundNavigationSerializers
import com.xwab.app.feature.story.navigation.storiesNavigationSerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Every route this app can put on a saved back stack, and how each one is written and read back.
 *
 * The app assembles this explicitly from the modules feature navigation packages export: a feature
 * knows its own route, and only the composition root knows which features this build has.
 *
 * A route registered with `appEntryProvider` but missing here fails only when a saved back stack is
 * restored, which is why FeatureSerializersTest checks both directions of every route.
 *
 * The reverse direction is [RetiredRoute]: a name in saved state that this build no longer
 * registers, which is what every route of a removed feature becomes.
 */
internal val FEATURE_SERIALIZERS: SerializersModule = SerializersModule {
    include(browseNavigationSerializers)
    include(favoritesNavigationSerializers)
    include(categoryNavigationSerializers)
    include(soundNavigationSerializers)
    include(storiesNavigationSerializers)

    // Not a feature's contribution, which is why this one is spelled out rather than included: it
    // is the app's answer for a name none of the five above registers. A saved back stack is
    // written by one build and read by the next, and a feature removed in between is exactly what
    // that looks like. Registered subclasses still win — the fallback is only consulted once the
    // lookup has already failed.
    polymorphic(NavKey::class) {
        subclass(RetiredRoute::class, RetiredRouteSerializer)
        defaultDeserializer { RetiredRouteSerializer }
    }
}
