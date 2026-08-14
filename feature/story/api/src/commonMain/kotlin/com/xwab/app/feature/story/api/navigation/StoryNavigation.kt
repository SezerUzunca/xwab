package com.xwab.app.feature.story.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * The one route this feature publishes: the story list.
 *
 * A `data object` rather than a route per story, because there is no story detail screen. Every
 * story is played from its row, so nothing has to be carried across a navigation.
 */
@Serializable
@SerialName("com.xwab.app.feature.story.navigation.StoriesRoute")
data object StoriesRoute : NavKey

val storyNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(StoriesRoute.serializer())
    }
}
