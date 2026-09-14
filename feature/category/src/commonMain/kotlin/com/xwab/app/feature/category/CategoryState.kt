package com.xwab.app.feature.category

import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.Track
import com.xwab.app.core.sound.port.TrackId

/** Content available after the outer [com.xwab.app.designsystem.state.Loadable] becomes ready. */
internal data class CategoryState(
    /**
     * The category this screen is about, or null when the catalog holds no such category.
     *
     * Null is an answer, not a gap to paper over. A route is a serialized string that outlives the
     * build that wrote it, so a back stack restored after the catalog changed can name a category
     * that is gone — and that used to draw a blank heading above "0 tracks", which reads as a
     * broken screen. Whoever renders this has to say so instead.
     */
    val category: Category? = null,
    val tracks: List<Track> = emptyList(),
    val favoriteIds: Set<TrackId> = emptySet(),
    val requestedTrackId: TrackId? = null,
    /** What the row's play/pause control shows: the session's intent, not audible sound. */
    val playIntent: Boolean = false,
    /** The requested sound is wanted but not audible yet. */
    val isPreparing: Boolean = false,
    /**
     * The session's failure, carried as the session reports it.
     *
     * A row here starts playback like a row on the favorites list does, so it reports what came of
     * that the same way. A failed lookup releases the session's claim, so by the time this arrives
     * [requestedTrackId] is no longer the track that failed; the row is found through [rowFailure].
     */
    val playbackFailure: PlaybackFailure? = null,
) {
    /**
     * What one row shows. Answered here rather than at the call site that draws it, so that the
     * question and the fields it reads stay in one file.
     */
    fun isRowPlaying(trackId: TrackId): Boolean = requestedTrackId == trackId && playIntent

    fun isRowPreparing(trackId: TrackId): Boolean = requestedTrackId == trackId && isPreparing

    fun rowFailure(trackId: TrackId): PlaybackFailure? =
        playbackFailure?.takeIf { it.itemId == PlaybackItemId.sound(trackId.value) }

    fun isRowFavorite(trackId: TrackId): Boolean = trackId in favoriteIds
}
