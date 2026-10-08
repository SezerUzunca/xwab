package com.xwab.app.feature.favorites.navigation

import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("com.xwab.app.feature.favorites.navigation.FavoritesRoute")
data object FavoritesRoute : NavKey

/** Contributes this feature's routes to the serializers saved back stacks are restored with. */
@ContributesTo(NavKey::class)
@BindingContainer
object FavoritesNavigationBindings {
    @Provides
    @IntoSet
    fun provideRouteSerializers(): SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(FavoritesRoute::class)
        }
    }
}
