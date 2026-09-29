package com.xwab.app.feature.nowplaying.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Stable saved route for the current session, rather than an item snapshot. */
@Serializable
@SerialName("com.xwab.app.feature.nowplaying.navigation.NowPlayingRoute")
data object NowPlayingRoute : NavKey

val nowPlayingNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(NowPlayingRoute::class)
    }
}
