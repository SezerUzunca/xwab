package com.xwab.app.core.story

import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId

/** Local story metadata. Physical addresses are owned by `:core:sources`. */
internal val storyManifest: List<Story> = listOf(
    Story(
        id = StoryId("night-came-slowly"),
        title = "The Night Came Slowly",
        author = "Kate Chopin",
        description = "A quiet meditation on dusk, stars, and the stillness of night.",
        narrator = "Alan Davis Drake",
        durationSeconds = 174,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("an-idle-fellow"),
        title = "An Idle Fellow",
        author = "Kate Chopin",
        description = "A brief character sketch about work, idleness, and how a life is judged.",
        narrator = "Alan Davis Drake",
        durationSeconds = 187,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("story-of-an-hour"),
        title = "The Story of an Hour",
        author = "Kate Chopin",
        description = "A woman receives sudden news and discovers how much can change in one hour.",
        narrator = "Alan Davis Drake",
        durationSeconds = 479,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("doctor-chevaliers-lie"),
        title = "Doctor Chevalier's Lie",
        author = "Kate Chopin",
        description = "A doctor offers one compassionate untruth after a lonely patient's final night.",
        narrator = "Alan Davis Drake",
        durationSeconds = 201,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("a-tent-in-agony"),
        title = "A Tent in Agony",
        author = "Stephen Crane",
        description = "Three friends trade an unsettling tale during a fishing trip.",
        narrator = "Alan Davis Drake",
        durationSeconds = 462,
        artworkUrl = null,
    ),
)
