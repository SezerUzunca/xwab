package com.xwab.app.feature.sounds.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object SoundsRoute : NavKey

val soundsNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SoundsRoute.serializer())
    }
}
