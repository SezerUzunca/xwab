package com.xwab.app.feature.cantsleep.navigation

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

/**
 * Named explicitly, because from the first release on this is a wire format: a saved back stack is
 * written with it, and restoring one throws rather than falling back when a name no longer
 * resolves. Left implicit the name follows the package, so moving this file would break the saved
 * navigation of every installed copy.
 */
@Serializable
@SerialName("com.xwab.app.feature.cantsleep.navigation.CantSleepRoute")
data object CantSleepRoute : NavKey

@ContributesTo(NavKey::class)
@BindingContainer
object CantSleepNavigationBindings {
    @Provides
    @IntoSet
    fun provideRouteSerializers(): SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(CantSleepRoute::class)
        }
    }
}
