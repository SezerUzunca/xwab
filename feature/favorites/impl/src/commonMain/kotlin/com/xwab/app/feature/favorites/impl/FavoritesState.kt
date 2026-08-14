package com.xwab.app.feature.favorites.impl

import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbacksession.PlaybackFailure

/** Content available after the outer [com.xwab.app.core.ui.state.Loadable] becomes ready. */
internal data class FavoritesState(
    val musics: List<Music> = emptyList(),
    val requestedTrackId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested favourite is wanted but not audible yet. */
    val isPreparing: Boolean = false,
    /**
     * The session's failure, carried as the session reports it.
     *
     * This used to be a local enum mirroring [PlaybackFailure] one-for-one, plus a separate
     * `failedTrackId` — both redundant, because a failure already names the item it happened to.
     * A failed lookup releases the session's claim, so by the time this arrives
     * [requestedTrackId] is no longer the track that failed; the row is found through
     * `playbackFailure.itemId` instead.
     */
    val playbackFailure: PlaybackFailure? = null,
)
