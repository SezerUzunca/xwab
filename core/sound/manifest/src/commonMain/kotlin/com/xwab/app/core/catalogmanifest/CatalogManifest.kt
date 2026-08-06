package com.xwab.app.core.catalogmanifest

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId

/**
 * Track metadata and the permanent HTTPS source behind it, in one hand-written list.
 *
 * The two live together so that adding a track is one edit in one file. What used to make that a
 * three-module change was the *other* kind of coupling — metadata in `core:data`, resource names in
 * the domain model, MP3s in `core:playback` — and none of those are split by capability.
 *
 * The list itself never leaves this module. Screens see it through [MusicCatalogRepository], and
 * `core:sound:delivery` sees only the physical half through [AudioSourceCatalog]; neither port can
 * be used to reach the other's business.
 *
 * Durations are the source recordings' own, so they no longer match the shorter, crossfaded copies
 * the app used to ship. Nothing is bundled any more: the app downloads a track when it is played.
 */
internal val catalogEntries = listOf(
    CatalogEntry(
        Music(TrackId("gentle-rain"), "Rain on the Window", "rain", 12, playbackTitle = "Gentle Rain"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/3d/Rain.ogg/Rain.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("calm-waves"), "Ontario Waves", "ocean", 1109, playbackTitle = "Calm Waves"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/1f/Waves.ogg/Waves.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("forest-birds"), "Fontainebleau Birds", "forest", 17, playbackTitle = "Forest Birds"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/38/Birds_forest.ogg/Birds_forest.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("white-noise"), "Soft White Noise", "white-noise", 20, playbackTitle = "White Noise"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/98/White-noise-sound-20sec-mono-44100Hz.ogg/White-noise-sound-20sec-mono-44100Hz.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("brahms-lullaby"), "Brahms' Lullaby", "lullaby", 81, playbackTitle = "Brahms' Lullaby"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/cf/Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg/Lullaby_wound_up_clock_guten_abend_gute_nacht.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("heavy-rain"), "Heavy Rain", "rain", 45, playbackArtist = "ezwa"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0e/Rain_(1).ogg/Rain_(1).ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("window-storm"), "Rain Against the Window", "rain", 82, playbackArtist = "cori"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/41/Rain_against_the_window.ogg/Rain_against_the_window.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("pebble-shore"), "Waves on a Pebble Beach", "ocean", 40, playbackArtist = "earthcalling"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/7/73/On_a_pebble_beach.ogg/On_a_pebble_beach.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("shorebirds"), "Shorebirds by the Sea", "ocean", 12, playbackArtist = "U.S. Fish and Wildlife Service"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/91/Shorebirds.ogg/Shorebirds.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("woodland-ambience"), "Woodland Ambience", "forest", 123, playbackArtist = "nille"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0a/20090610_0_ambience.ogg/20090610_0_ambience.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("chorus-cicadas"), "Chorus Cicadas", "forest", 24, playbackArtist = "Siobhan Leachman"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/b/b1/Chorus_Cicada_singing.ogg/Chorus_Cicada_singing.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("brown-noise"), "Soft Brown Noise", "white-noise", 10),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c9/Brownnoise.ogg/Brownnoise.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("pink-noise"), "Soft Pink Noise", "white-noise", 10, playbackArtist = "Bautsch"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a2/Pink.Noise.ogg/Pink.Noise.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("chopin-berceuse"), "Chopin's Berceuse", "lullaby", 290, playbackArtist = "Veronica van der Knaap"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/d/d5/Chopin-Berceuse.ogg/Chopin-Berceuse.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("igbo-lullaby"), "Igbo Lullaby", "lullaby", 37, playbackTitle = "Egwu Nwa", playbackArtist = "Akum20"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/d/d9/Egwu_Nwa.ogg/Egwu_Nwa.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("thunder-rain"), "Rain and Thunder", "rain", 19, playbackArtist = "Caesar"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/42/Rain_and_thunder.ogg/Rain_and_thunder.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("south-carolina-beach"), "South Carolina Beach", "ocean", 62, playbackArtist = "Anthropic42"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/04/Beach_sounds_South_Carolina.ogg/Beach_sounds_South_Carolina.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("nightingale-song"), "Nightingale Song", "forest", 151, playbackArtist = "Digweed1"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/9/91/Common_Nightingale%27s_song_2.ogg/Common_Nightingale%27s_song_2.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("gray-noise"), "Soft Gray Noise", "white-noise", 10, playbackArtist = "Omegatron"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c0/Gray_noise.ogg/Gray_noise.ogg.mp3",
    ),
    CatalogEntry(
        Music(TrackId("vierne-berceuse"), "Vierne Berceuse", "lullaby", 269, playbackArtist = "Vox Mirabilis"),
        "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c0/Vierne_Berceuse_%28Fernwerk_und_Hauptorgel%29.ogg/Vierne_Berceuse_%28Fernwerk_und_Hauptorgel%29.ogg.mp3",
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
