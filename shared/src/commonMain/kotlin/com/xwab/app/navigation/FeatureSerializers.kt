package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metro.createGraph
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Every route this app can put on a saved back stack, and how each one is written and read back.
 *
 * Each feature contributes its own module through Metro, so a feature on shared's classpath is
 * registered without being listed here.
 *
 * A route with an entry but no registration fails only when a saved back stack is restored.
 * checkArchitecture fails the build instead: every route a feature declares must be registered in the
 * module its feature contributes. FeatureSerializersTest checks the wire format of the routes that are.
 *
 * The reverse direction is [RetiredRoute]: a name in saved state that this build no longer
 * registers, which is what every route of a removed feature becomes.
 */
internal val FEATURE_SERIALIZERS: SerializersModule = SerializersModule {
    createGraph<RouteSerializersGraph>().routeSerializers.forEach { include(it) }

    // Not a feature's contribution, which is why this one is spelled out rather than included: it
    // is the app's answer for a name none of the installed features registers. A saved back stack is
    // written by one build and read by the next, and a feature removed in between is exactly what
    // that looks like. Registered subclasses still win — the fallback is only consulted once the
    // lookup has already failed.
    polymorphic(NavKey::class) {
        subclass(RetiredRoute::class, RetiredRouteSerializer)
        defaultDeserializer { RetiredRouteSerializer }
    }
}

/**
 * A route's identity as its saved form spells it: its serial name, then its argument values.
 *
 * Built the way the back stack itself is saved, so it survives renaming a route's class, which the
 * `toString()` behind Navigation 3's default content key does not. Each value is length-prefixed,
 * so no argument can imitate a separator.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun NavKey.savedIdentity(): String {
    val serializer = requireNotNull(FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, this)) {
        "${this::class.simpleName} is not registered in FEATURE_SERIALIZERS"
    }
    val identity = StringBuilder(serializer.descriptor.serialName)
    val encoder = object : AbstractEncoder() {
        override val serializersModule: SerializersModule = FEATURE_SERIALIZERS

        override fun encodeValue(value: Any) {
            val text = value.toString()
            identity.append('|').append(text.length).append(':').append(text)
        }

        override fun encodeNull() {
            identity.append("|null")
        }
    }
    serializer.serialize(encoder, this)
    return identity.toString()
}
