package com.xwab.app.core.storymanifest

import com.xwab.app.core.story.Story
import com.xwab.app.core.story.StoryId

/**
 * The stories the app knows about, in one hand-written list.
 *
 * **These are placeholders.** Nothing here is recorded audio yet: the entries exist so that the
 * `StoryCatalogRepository` port has something to serve, and so that the modules around it —
 * DI, architecture rules, a future `feature:story` — can be built and tested before a story feed
 * exists. When it does, this list is what a feed-backed implementation replaces, and no module
 * outside this one changes shape.
 *
 * There is no source URL here on purpose. Story audio is online-only and must never enter the sound
 * cache in `core:sound:delivery`, whose resolver starts a background download whenever it misses.
 * Where a story's bytes come from is decided with the feed contract — endpoint, whether URLs are
 * permanent or signed, and how long a signed one lives — and lands in this module, beside the list.
 *
 * The list never leaves the module. Screens see it through `StoryCatalogRepository`, whose
 * interface lives in `core:story:catalog`, and no feature declares this module.
 */
internal val storyManifest: List<Story> = listOf(
    Story(
        id = StoryId("forest-lantern"),
        title = "The Forest Lantern",
        description = "A slow walk through a winter forest, one lantern at a time.",
        narrator = "Mira",
        durationSeconds = 900,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("harbour-night"),
        title = "Harbour at Night",
        description = "Rope, water and a sleeping fishing town.",
        narrator = "Elias",
        durationSeconds = 1200,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("long-train"),
        title = "The Long Train",
        description = "A night train crossing an empty plain, carriage by carriage.",
        narrator = "Mira",
        durationSeconds = 1500,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("lighthouse-keeper"),
        title = "The Lighthouse Keeper",
        description = "One quiet shift, told from the top of the stairs.",
        narrator = "Elias",
        durationSeconds = 1080,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("garden-after-rain"),
        title = "The Garden After Rain",
        description = "Everything dripping, nothing in a hurry.",
        narrator = null,
        durationSeconds = 720,
        artworkUrl = null,
    ),
    Story(
        id = StoryId("paper-boats"),
        title = "Paper Boats",
        description = "A child sends folded boats downstream until the light goes.",
        narrator = "Mira",
        durationSeconds = 840,
        artworkUrl = null,
    ),
)
