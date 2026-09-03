package com.xwab.app.feature.browse.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Root route for browsing the sound catalog. */
@Serializable
@SerialName("com.xwab.app.feature.home.navigation.HomeRoute")
data object BrowseRoute : NavKey

val browseNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(BrowseRoute.serializer())
    }
}
