package com.xwab.app.core.sound

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId

/** Local sound metadata. Physical addresses are owned by `:core:sources`. */
internal val catalogManifest = listOf(
    Track(TrackId("gentle-rain"), "Rain on the Window", CategoryId("rain"), 12, playbackTitle = "Gentle Rain"),
    Track(TrackId("calm-waves"), "Ontario Waves", CategoryId("ocean"), 1109, playbackTitle = "Calm Waves"),
    Track(TrackId("forest-birds"), "Fontainebleau Birds", CategoryId("forest"), 17, playbackTitle = "Forest Birds"),
    Track(TrackId("white-noise"), "Soft White Noise", CategoryId("white-noise"), 20, playbackTitle = "White Noise"),
    Track(TrackId("brahms-lullaby"), "Brahms' Lullaby", CategoryId("lullaby"), 81, playbackTitle = "Brahms' Lullaby"),
    Track(TrackId("heavy-rain"), "Heavy Rain", CategoryId("rain"), 45, playbackArtist = "ezwa"),
    Track(TrackId("window-storm"), "Rain Against the Window", CategoryId("rain"), 82, playbackArtist = "cori"),
    Track(TrackId("pebble-shore"), "Waves on a Pebble Beach", CategoryId("ocean"), 40, playbackArtist = "earthcalling"),
    Track(TrackId("shorebirds"), "Shorebirds by the Sea", CategoryId("ocean"), 12, playbackArtist = "U.S. Fish and Wildlife Service"),
    Track(TrackId("woodland-ambience"), "Woodland Ambience", CategoryId("forest"), 123, playbackArtist = "nille"),
    Track(TrackId("chorus-cicadas"), "Chorus Cicadas", CategoryId("forest"), 24, playbackArtist = "Siobhan Leachman"),
    Track(TrackId("brown-noise"), "Soft Brown Noise", CategoryId("white-noise"), 10),
    Track(TrackId("pink-noise"), "Soft Pink Noise", CategoryId("white-noise"), 10, playbackArtist = "Bautsch"),
    Track(TrackId("chopin-berceuse"), "Chopin's Berceuse", CategoryId("lullaby"), 290, playbackArtist = "Veronica van der Knaap"),
    Track(TrackId("igbo-lullaby"), "Igbo Lullaby", CategoryId("lullaby"), 37, playbackTitle = "Egwu Nwa", playbackArtist = "Akum20"),
    Track(TrackId("thunder-rain"), "Rain and Thunder", CategoryId("rain"), 19, playbackArtist = "Caesar"),
    Track(TrackId("south-carolina-beach"), "South Carolina Beach", CategoryId("ocean"), 62, playbackArtist = "Anthropic42"),
    Track(TrackId("nightingale-song"), "Nightingale Song", CategoryId("forest"), 151, playbackArtist = "Digweed1"),
    Track(TrackId("gray-noise"), "Soft Gray Noise", CategoryId("white-noise"), 10, playbackArtist = "Omegatron"),
    Track(TrackId("vierne-berceuse"), "Vierne Berceuse", CategoryId("lullaby"), 269, playbackArtist = "Vox Mirabilis"),
)

internal val catalogCategories = listOf(
    Category(CategoryId("rain"), "Rain", "Gentle raindrops", "\u2602", 0),
    Category(CategoryId("ocean"), "Ocean", "Calming waves", "\u2248", 0),
    Category(CategoryId("forest"), "Forest", "Birds and nature", "\u2667", 0),
    Category(CategoryId("white-noise"), "White Noise", "Uninterrupted calm", "\u25cc", 0),
    Category(CategoryId("lullaby"), "Lullabies", "Peace for all ages", "\u263e", 0),
).map { category ->
    category.copy(trackCount = catalogManifest.count { it.categoryId == category.id })
}
