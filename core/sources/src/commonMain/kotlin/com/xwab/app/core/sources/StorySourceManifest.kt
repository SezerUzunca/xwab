package com.xwab.app.core.sources

import com.xwab.app.core.sources.port.ContentSource

internal val storySourceManifest = listOf(
    storySource("night-came-slowly", "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/26/The_Night_Came_Slowly_Chopin.ogg/The_Night_Came_Slowly_Chopin.ogg.mp3"),
    storySource("an-idle-fellow", "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/bd/KateChopin_AnIdleFellow.ogg/KateChopin_AnIdleFellow.ogg.mp3"),
    storySource("story-of-an-hour", "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/36/The_Story_of_an_Hour_Chopin.ogg/The_Story_of_an_Hour_Chopin.ogg.mp3"),
    storySource("doctor-chevaliers-lie", "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/90/KateChopin_DrChevaliersLie.ogg/KateChopin_DrChevaliersLie.ogg.mp3"),
    storySource("a-tent-in-agony", "https://upload.wikimedia.org/wikipedia/commons/transcoded/f/f9/ATentInAgony_Crane_add_Stephen_Crane.ogg/ATentInAgony_Crane_add_Stephen_Crane.ogg.mp3"),
)

private fun storySource(itemId: String, httpsUrl: String): ManifestSource =
    ManifestSource(itemId = itemId, source = ContentSource(httpsUrl = httpsUrl))
