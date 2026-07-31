package com.xwab.app.core.audiocontent

import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music

/**
 * The catalog and its physical source manifest intentionally live together, in the same module as
 * the code that fetches and caches them.
 *
 * Adding a track therefore cannot create the former hidden coupling where metadata lived in
 * `core:data`, resource names in the domain model, and the MP3 files in `core:playback`.
 * [MusicCatalogRepository] serves the metadata half of it; the sources stay internal.
 *
 * Durations are the source recordings' own, so they no longer match the shorter, crossfaded copies
 * the app used to ship. Nothing is bundled any more: the app downloads a track when it is played.
 */
internal val catalogEntries = listOf(
    CatalogEntry(
        Music("gentle-rain", "Rain on the Window", "rain", 12, playbackTitle = "Gentle Rain"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/3d/Rain.ogg/Rain.ogg.mp3",
    ),
    CatalogEntry(
        Music("calm-waves", "Ontario Waves", "ocean", 1109, playbackTitle = "Calm Waves"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/1f/Waves.ogg/Waves.ogg.mp3",
    ),
    CatalogEntry(
        Music("forest-birds", "Fontainebleau Birds", "forest", 17, playbackTitle = "Forest Birds"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/38/Birds_forest.ogg/Birds_forest.ogg.mp3",
    ),
    CatalogEntry(
        Music("white-noise", "Soft White Noise", "white-noise", 20, playbackTitle = "White Noise"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/98/White-noise-sound-20sec-mono-44100Hz.ogg/White-noise-sound-20sec-mono-44100Hz.ogg.mp3",
    ),
    CatalogEntry(
        Music("brahms-lullaby", "Brahms' Lullaby", "lullaby", 81, playbackTitle = "Brahms' Lullaby"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/cf/Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg/Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg.mp3",
    ),
    CatalogEntry(
        Music("heavy-rain", "Heavy Rain", "rain", 45, playbackArtist = "ezwa"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0e/Rain_(1).ogg/Rain_(1).ogg.mp3",
    ),
    CatalogEntry(
        Music("window-storm", "Rain Against the Window", "rain", 82, playbackArtist = "cori"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/Rain_against_the_window.ogg/Rain_against_the_window.ogg.mp3",
    ),
    CatalogEntry(
        Music("pebble-shore", "Waves on a Pebble Beach", "ocean", 40, playbackArtist = "earthcalling"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/7/73/On_a_pebble_beach.ogg/On_a_pebble_beach.ogg.mp3",
    ),
    CatalogEntry(
        Music("shorebirds", "Shorebirds by the Sea", "ocean", 12, playbackArtist = "U.S. Fish and Wildlife Service"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/91/Shorebirds.ogg/Shorebirds.ogg.mp3",
    ),
    CatalogEntry(
        Music("woodland-ambience", "Woodland Ambience", "forest", 123, playbackArtist = "nille"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0a/20090610_0_ambience.ogg/20090610_0_ambience.ogg.mp3",
    ),
    CatalogEntry(
        Music("chorus-cicadas", "Chorus Cicadas", "forest", 24, playbackArtist = "Siobhan Leachman"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/b1/Chorus_Cicada_singing.ogg/Chorus_Cicada_singing.ogg.mp3",
    ),
    CatalogEntry(
        Music("brown-noise", "Soft Brown Noise", "white-noise", 10),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c9/Brownnoise.ogg/Brownnoise.ogg.mp3",
    ),
    CatalogEntry(
        Music("pink-noise", "Soft Pink Noise", "white-noise", 10, playbackArtist = "Bautsch"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a2/Pink.Noise.ogg/Pink.Noise.ogg.mp3",
    ),
    CatalogEntry(
        Music("chopin-berceuse", "Chopin's Berceuse", "lullaby", 290, playbackArtist = "Veronica van der Knaap"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/d/d5/Chopin-Berceuse.ogg/Chopin-Berceuse.ogg.mp3",
    ),
    CatalogEntry(
        Music("igbo-lullaby", "Igbo Lullaby", "lullaby", 37, playbackTitle = "Egwu Nwa", playbackArtist = "Akum20"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/d/d9/Egwu_Nwa.ogg/Egwu_Nwa.ogg.mp3",
    ),
)

internal val catalogCategories = listOf(
    Category("rain", "Rain", "Gentle raindrops", "\u2602", 0),
    Category("ocean", "Ocean", "Calming waves", "\u2248", 0),
    Category("forest", "Forest", "Birds and nature", "\u2667", 0),
    Category("white-noise", "White Noise", "Uninterrupted calm", "\u25cc", 0),
    Category("lullaby", "Lullabies", "Peace for all ages", "\u263e", 0),
).map { category ->
    category.copy(musicCount = catalogEntries.count { it.music.categoryId == category.id })
}
