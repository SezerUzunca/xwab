package com.xwab.app.feature.story.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** The stories tab's stable root route. */
@Serializable
@SerialName("com.xwab.app.feature.story.navigation.StoriesRoute")
data object StoriesRoute : NavKey

/** A story's metadata and item-specific playback action. */
@Serializable
@SerialName("com.xwab.app.feature.story.navigation.StoryRoute")
data class StoryRoute(val storyId: String) : NavKey

val storiesNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(StoriesRoute::class)
        subclass(StoryRoute::class)
    }
}
