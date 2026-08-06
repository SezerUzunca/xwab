package com.xwab.app.feature.home

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId

internal data class HomeState(
    val categories: List<Category> = emptyList(),
    val favoriteMusics: List<Music> = emptyList(),
    val playingMusicId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
)
