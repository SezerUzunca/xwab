package com.xwab.app.feature.sounds.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("com.xwab.app.feature.sounds.navigation.PlayerRoute")
data class PlayerRoute(val musicId: String) : NavKey

val soundsNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(PlayerRoute.serializer())
    }
}
