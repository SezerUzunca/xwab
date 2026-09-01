package com.xwab.app.navigation

import com.xwab.app.feature.category.api.navigation.CategoryConfig
import com.xwab.app.feature.sounds.api.navigation.PlayerConfig
import kotlinx.serialization.Serializable

/** The Browse tab's own back stack: its root, or wherever browsing a category leads. */
@Serializable
internal sealed interface BrowseTabConfig {
    @Serializable
    data object Root : BrowseTabConfig

    @Serializable
    data class Category(val config: CategoryConfig) : BrowseTabConfig

    @Serializable
    data class Player(val config: PlayerConfig) : BrowseTabConfig
}
