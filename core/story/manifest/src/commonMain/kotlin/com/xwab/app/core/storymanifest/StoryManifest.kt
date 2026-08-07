package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId

/**
 * The stories the app knows about, in one hand-written list.
 *
 * The shape mirrors `catalogEntries` on the sound side: metadata and source in one row, so adding a
 * story is one edit in one file. The difference is the source itself. A sound row names a file to
 * cache; a story row names an address to stream, because story audio must never enter the cache in
 * `core:sound:delivery`, whose resolver starts a background download whenever it misses.
 *
 * The list never leaves the module. Screens see it through `StoryCatalogRepository`, whose interface
 * lives in `core:story:catalog`, and the session sees only the source half through
 * [StoryStreamCatalog]; neither port can be used to reach the other's business. Recording sources
 * and public-domain grants are audited in the repository's `THIRD_PARTY_AUDIO.md`.
 */
internal val storyManifest: List<StoryEntry> = listOf(
    StoryEntry(
        story = Story(
            id = StoryId("night-came-slowly"),
            title = "The Night Came Slowly",
            author = "Kate Chopin",
            description = "A quiet meditation on dusk, stars, and the stillness of night.",
            narrator = "Alan Davis Drake",
            durationSeconds = 174,
            artworkUrl = null,
        ),
        httpsUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/26/" +
            "The_Night_Came_Slowly_Chopin.ogg/The_Night_Came_Slowly_Chopin.ogg.mp3",
    ),
    StoryEntry(
        story = Story(
            id = StoryId("an-idle-fellow"),
            title = "An Idle Fellow",
            author = "Kate Chopin",
            description = "A brief character sketch about work, idleness, and how a life is judged.",
            narrator = "Alan Davis Drake",
            durationSeconds = 187,
            artworkUrl = null,
        ),
        httpsUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/bd/" +
            "KateChopin_AnIdleFellow.ogg/KateChopin_AnIdleFellow.ogg.mp3",
    ),
    StoryEntry(
        story = Story(
            id = StoryId("story-of-an-hour"),
            title = "The Story of an Hour",
            author = "Kate Chopin",
            description = "A woman receives sudden news and discovers how much can change in one hour.",
            narrator = "Alan Davis Drake",
            durationSeconds = 479,
            artworkUrl = null,
        ),
        httpsUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/36/" +
            "The_Story_of_an_Hour_Chopin.ogg/The_Story_of_an_Hour_Chopin.ogg.mp3",
    ),
    StoryEntry(
        story = Story(
            id = StoryId("doctor-chevaliers-lie"),
            title = "Doctor Chevalier's Lie",
            author = "Kate Chopin",
            description = "A doctor offers one compassionate untruth after a lonely patient's final night.",
            narrator = "Alan Davis Drake",
            durationSeconds = 201,
            artworkUrl = null,
        ),
        httpsUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/90/" +
            "KateChopin_DrChevaliersLie.ogg/KateChopin_DrChevaliersLie.ogg.mp3",
    ),
    StoryEntry(
        story = Story(
            id = StoryId("a-tent-in-agony"),
            title = "A Tent in Agony",
            author = "Stephen Crane",
            description = "Three friends trade an unsettling tale during a fishing trip.",
            narrator = "Alan Davis Drake",
            durationSeconds = 462,
            artworkUrl = null,
        ),
        httpsUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/f/f9/" +
            "ATentInAgony_Crane_add_Stephen_Crane.ogg/" +
            "ATentInAgony_Crane_add_Stephen_Crane.ogg.mp3",
    ),
)

/** The stories themselves, which is all the repository serves. */
internal val storyCatalog: List<Story> = storyManifest.map(StoryEntry::story)
