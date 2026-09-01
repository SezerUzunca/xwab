package com.xwab.app.feature.sounds.api.navigation

import kotlinx.serialization.Serializable

/** Which sound to play. */
@Serializable
data class PlayerConfig(val musicId: String)
