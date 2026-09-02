package com.xwab.app.navigation

import com.xwab.app.feature.sounds.api.navigation.PlayerConfig
import kotlinx.serialization.Serializable

/** The Favorites tab's own back stack: its root, or wherever a favorite leads. */
@Serializable
internal sealed interface FavoritesTabConfig {
    @Serializable
    data object Root : FavoritesTabConfig

    @Serializable
    data class Player(val config: PlayerConfig) : FavoritesTabConfig
}
