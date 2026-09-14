package com.xwab.app.feature.browse.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Root route for browsing the sound catalog.
 *
 * The serial name read `com.xwab.app.feature.home.navigation.HomeRoute` until now, after the
 * feature this one replaced. These names are a wire format — a saved back stack names its routes
 * with them, and `rememberNavBackStack` throws on restore rather than falling back when one no
 * longer resolves — so from the first release on, a rename here costs a failed restore.
 */
@Serializable
@SerialName("com.xwab.app.feature.browse.navigation.BrowseRoute")
data object BrowseRoute : NavKey

val browseNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(BrowseRoute.serializer())
    }
}
