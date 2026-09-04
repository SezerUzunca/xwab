package com.xwab.app.feature.favorites.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("com.xwab.app.feature.favorites.api.navigation.FavoritesRoute")
data object FavoritesRoute : NavKey

val favoritesNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(FavoritesRoute.serializer())
    }
}
