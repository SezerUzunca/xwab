package com.xwab.app.feature.category.navigation

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.navigation.Navigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class CategoryRoute(val categoryId: String) : NavKey

val categoryNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CategoryRoute.serializer())
    }
}

fun Navigator.navigateToCategory(categoryId: String) {
    navigate(CategoryRoute(categoryId))
}
