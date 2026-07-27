package com.xwab.app.feature.home

import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music

internal data class HomeState(
    val categories: List<Category> = emptyList(),
    val favoriteMusics: List<Music> = emptyList(),
    val playingMusicId: String? = null,
    val isPlaying: Boolean = false,
)
