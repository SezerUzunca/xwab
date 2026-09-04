package com.xwab.app.feature.category.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("com.xwab.app.feature.category.navigation.CategoryRoute")
data class CategoryRoute(val categoryId: String) : NavKey

val categoryNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CategoryRoute.serializer())
    }
}
