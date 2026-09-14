package com.xwab.app.feature.favorites

import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId

/** Content available after the outer [com.xwab.app.designsystem.state.Loadable] becomes ready. */
internal data class FavoritesState(
    val tracks: List<Track> = emptyList(),
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
     * [rowFailure] instead.
     */
    val playbackFailure: PlaybackFailure? = null,
) {
    /**
     * What one row shows. Answered here rather than at the call site that draws it, so that the
     * question and the fields it reads stay in one file.
     *
     * The three used to be spelled out inside the composable, and [rowFailure] in particular
     * compared raw id strings — safe only because the ViewModel had already dropped failures of
     * another kind, a guarantee that lived in a different file. It matches the whole item id now,
     * so it holds on its own.
     */
    fun isRowPlaying(trackId: TrackId): Boolean = requestedTrackId == trackId && playIntent

    fun isRowPreparing(trackId: TrackId): Boolean = requestedTrackId == trackId && isPreparing

    fun rowFailure(trackId: TrackId): PlaybackFailure? =
        playbackFailure?.takeIf { it.itemId == PlaybackItemId.sound(trackId.value) }
}
