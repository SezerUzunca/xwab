package com.xwab.app.feature.sound.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * The route to one sound's own screen.
 *
 * A property name here is a serialized field name, so from the first release on this is a wire
 * format: a saved back stack is written with it, and `rememberNavBackStack` throws on restore
 * rather than falling back when a name no longer resolves. This one read `musicId` until the
 * catalog model stopped being called `Music`, and could follow it only because nothing has shipped
 * holding the old spelling.
 */
@Serializable
@SerialName("com.xwab.app.feature.sound.navigation.SoundRoute")
data class SoundRoute(val trackId: String) : NavKey

val soundNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SoundRoute.serializer())
    }
}
