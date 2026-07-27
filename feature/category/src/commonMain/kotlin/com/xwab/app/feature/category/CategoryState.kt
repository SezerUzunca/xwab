package com.xwab.app.feature.category

import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music

internal data class CategoryState(
    val category: Category? = null,
    val musics: List<Music> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val playingMusicId: String? = null,
    val isPlaying: Boolean = false,
)
