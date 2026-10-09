package com.xwab.app.core.story

/** Physical stream location owned and consumed only by the story module. */
internal data class StorySource(val itemId: String, val httpsUrl: String) {
    init {
        require(itemId.isNotBlank()) { "Story source ids must not be blank." }
        require(httpsUrl.matches(Regex("https://[^\\s/?#]+(?:[/?#]\\S*)?"))) {
            "Story sources must use an HTTPS address."
        }
    }
}

internal val storySourceManifest = listOf(
    StorySource("night-came-slowly", "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/26/The_Night_Came_Slowly_Chopin.ogg/The_Night_Came_Slowly_Chopin.ogg.mp3"),
    StorySource("an-idle-fellow", "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/bd/KateChopin_AnIdleFellow.ogg/KateChopin_AnIdleFellow.ogg.mp3"),
    StorySource("story-of-an-hour", "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/36/The_Story_of_an_Hour_Chopin.ogg/The_Story_of_an_Hour_Chopin.ogg.mp3"),
    StorySource("doctor-chevaliers-lie", "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/90/KateChopin_DrChevaliersLie.ogg/KateChopin_DrChevaliersLie.ogg.mp3"),
    StorySource("a-tent-in-agony", "https://upload.wikimedia.org/wikipedia/commons/transcoded/f/f9/ATentInAgony_Crane_add_Stephen_Crane.ogg/ATentInAgony_Crane_add_Stephen_Crane.ogg.mp3"),
)
