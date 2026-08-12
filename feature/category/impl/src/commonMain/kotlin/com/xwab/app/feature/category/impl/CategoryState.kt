package com.xwab.app.feature.category.impl

import com.xwab.app.core.catalog.Category
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId

/** Content available after the outer [com.xwab.app.core.ui.state.Loadable] becomes ready. */
internal data class CategoryState(
    val category: Category? = null,
    val musics: List<Music> = emptyList(),
    val favoriteIds: Set<TrackId> = emptySet(),
    val requestedTrackId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
)
