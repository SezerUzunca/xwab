package com.xwab.app.feature.home.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object HomeRoute : NavKey

val homeNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(HomeRoute.serializer())
    }
}
