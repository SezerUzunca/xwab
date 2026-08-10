package com.xwab.app.feature.sounds

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId

internal data class SoundsState(
    val categories: List<Category> = emptyList(),
    val favoriteMusics: List<Music> = emptyList(),
    val requestedTrackId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
)
