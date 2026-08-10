package com.xwab.app.feature.player.navigation

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class PlayerRoute(val musicId: String) : NavKey

val playerNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(PlayerRoute.serializer())
    }
}

fun Navigator.navigateToPlayer(musicId: String) {
    navigate(PlayerRoute(musicId))
}
