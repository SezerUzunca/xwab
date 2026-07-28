package com.xwab.app.core.data.repository

import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Serves the catalog shipped inside the app bundle; there is no remote or on-device source. */
internal class BundledMusicCatalogRepository : MusicCatalogRepository {
    private val tracks = listOf(
        Music("gentle-rain", "Rain on the Window", "rain", 9, "files/audio/gentle_rain.mp3", playbackTitle = "Gentle Rain"),
        Music("calm-waves", "Ontario Waves", "ocean", 286, "files/audio/calm_waves.mp3", playbackTitle = "Calm Waves"),
        Music("forest-birds", "Fontainebleau Birds", "forest", 13, "files/audio/forest_birds.mp3", playbackTitle = "Forest Birds"),
        Music("white-noise", "Soft White Noise", "white-noise", 19, "files/audio/white_noise.mp3", playbackTitle = "White Noise"),
        Music("brahms-lullaby", "Brahms' Lullaby", "lullaby", 45, "files/audio/brahms_lullaby.mp3", playbackTitle = "Brahms' Lullaby"),
    )

    private val categoryDefinitions = listOf(
        Category("rain", "Rain", "Gentle raindrops", "\u2602", 0),
        Category("ocean", "Ocean", "Calming waves", "\u2248", 0),
        Category("forest", "Forest", "Birds and nature", "\u2667", 0),
        Category("white-noise", "White Noise", "Uninterrupted calm", "\u25cc", 0),
        Category("lullaby", "Lullabies", "Peace for all ages", "\u263e", 0),
    )

    private val categories = categoryDefinitions.map { category ->
        category.copy(musicCount = tracks.count { it.categoryId == category.id })
    }
    private val categoryStream = flowOf(categories)
    private val trackStream = flowOf(tracks)

    override fun observeCategories(): Flow<List<Category>> = categoryStream
    override fun observeAllMusic(): Flow<List<Music>> = trackStream
    override fun observeCategory(categoryId: String): Flow<Category?> = categoryStream.map { categories -> categories.find { it.id == categoryId } }
    override fun observeMusicForCategory(categoryId: String): Flow<List<Music>> = trackStream.map { tracks -> tracks.filter { it.categoryId == categoryId } }
    override fun observeMusic(musicId: String): Flow<Music?> = trackStream.map { tracks -> tracks.find { it.id == musicId } }
}
