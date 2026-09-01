package com.xwab.app.feature.story.api.navigation

import kotlinx.serialization.Serializable

/**
 * Marks the story list's place in whichever tab hosts it.
 *
 * A plain marker rather than one config per story, because there is no story detail screen. Every
 * story is played from its row, so nothing has to be carried across a navigation.
 */
@Serializable
data object StoriesConfig
