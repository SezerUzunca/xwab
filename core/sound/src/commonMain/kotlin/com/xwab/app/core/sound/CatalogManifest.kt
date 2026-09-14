package com.xwab.app.core.sound

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.TrackId

/** Local sound metadata. Physical addresses are owned by `:core:sources`. */
internal val catalogManifest = listOf(
    Music(TrackId("gentle-rain"), "Rain on the Window", CategoryId("rain"), 12, playbackTitle = "Gentle Rain"),
    Music(TrackId("calm-waves"), "Ontario Waves", CategoryId("ocean"), 1109, playbackTitle = "Calm Waves"),
    Music(TrackId("forest-birds"), "Fontainebleau Birds", CategoryId("forest"), 17, playbackTitle = "Forest Birds"),
    Music(TrackId("white-noise"), "Soft White Noise", CategoryId("white-noise"), 20, playbackTitle = "White Noise"),
    Music(TrackId("brahms-lullaby"), "Brahms' Lullaby", CategoryId("lullaby"), 81, playbackTitle = "Brahms' Lullaby"),
    Music(TrackId("heavy-rain"), "Heavy Rain", CategoryId("rain"), 45, playbackArtist = "ezwa"),
    Music(TrackId("window-storm"), "Rain Against the Window", CategoryId("rain"), 82, playbackArtist = "cori"),
    Music(TrackId("pebble-shore"), "Waves on a Pebble Beach", CategoryId("ocean"), 40, playbackArtist = "earthcalling"),
    Music(TrackId("shorebirds"), "Shorebirds by the Sea", CategoryId("ocean"), 12, playbackArtist = "U.S. Fish and Wildlife Service"),
    Music(TrackId("woodland-ambience"), "Woodland Ambience", CategoryId("forest"), 123, playbackArtist = "nille"),
    Music(TrackId("chorus-cicadas"), "Chorus Cicadas", CategoryId("forest"), 24, playbackArtist = "Siobhan Leachman"),
    Music(TrackId("brown-noise"), "Soft Brown Noise", CategoryId("white-noise"), 10),
    Music(TrackId("pink-noise"), "Soft Pink Noise", CategoryId("white-noise"), 10, playbackArtist = "Bautsch"),
    Music(TrackId("chopin-berceuse"), "Chopin's Berceuse", CategoryId("lullaby"), 290, playbackArtist = "Veronica van der Knaap"),
    Music(TrackId("igbo-lullaby"), "Igbo Lullaby", CategoryId("lullaby"), 37, playbackTitle = "Egwu Nwa", playbackArtist = "Akum20"),
    Music(TrackId("thunder-rain"), "Rain and Thunder", CategoryId("rain"), 19, playbackArtist = "Caesar"),
    Music(TrackId("south-carolina-beach"), "South Carolina Beach", CategoryId("ocean"), 62, playbackArtist = "Anthropic42"),
    Music(TrackId("nightingale-song"), "Nightingale Song", CategoryId("forest"), 151, playbackArtist = "Digweed1"),
    Music(TrackId("gray-noise"), "Soft Gray Noise", CategoryId("white-noise"), 10, playbackArtist = "Omegatron"),
    Music(TrackId("vierne-berceuse"), "Vierne Berceuse", CategoryId("lullaby"), 269, playbackArtist = "Vox Mirabilis"),
)

internal val catalogCategories = listOf(
    Category(CategoryId("rain"), "Rain", "Gentle raindrops", "\u2602", 0),
    Category(CategoryId("ocean"), "Ocean", "Calming waves", "\u2248", 0),
    Category(CategoryId("forest"), "Forest", "Birds and nature", "\u2667", 0),
    Category(CategoryId("white-noise"), "White Noise", "Uninterrupted calm", "\u25cc", 0),
    Category(CategoryId("lullaby"), "Lullabies", "Peace for all ages", "\u263e", 0),
).map { category ->
    category.copy(musicCount = catalogManifest.count { it.categoryId == category.id })
}
