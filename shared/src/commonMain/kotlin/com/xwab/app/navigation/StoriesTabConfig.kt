package com.xwab.app.navigation

import kotlinx.serialization.Serializable

/**
 * The Stories tab's own back stack: just its root.
 *
 * A story is played from its row and has no screen of its own, so nothing is ever pushed here.
 */
@Serializable
internal sealed interface StoriesTabConfig {
    @Serializable
    data object Root : StoriesTabConfig
}
