package com.xwab.app.home

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * The shell's start destination.
 *
 * No `navigateToHome` extension: nothing routes *to* home. It is a tab, and `Navigator.navigate`
 * switches to a tab by its route — which the navigation bar already holds.
 */
@Serializable
internal data object HomeRoute : NavKey

internal val homeNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(HomeRoute.serializer())
    }
}
