package com.xwab.app.feature.category

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId

internal data class CategoryState(
    val category: Category? = null,
    val musics: List<Music> = emptyList(),
    val favoriteIds: Set<TrackId> = emptySet(),
    val playingMusicId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
)
