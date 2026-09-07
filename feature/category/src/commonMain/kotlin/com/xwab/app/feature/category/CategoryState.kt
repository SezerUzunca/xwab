package com.xwab.app.feature.category

import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.TrackId

/** Content available after the outer [com.xwab.app.designsystem.state.Loadable] becomes ready. */
internal data class CategoryState(
    val category: Category? = null,
    val musics: List<Music> = emptyList(),
    val favoriteIds: Set<TrackId> = emptySet(),
    val requestedTrackId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
)
