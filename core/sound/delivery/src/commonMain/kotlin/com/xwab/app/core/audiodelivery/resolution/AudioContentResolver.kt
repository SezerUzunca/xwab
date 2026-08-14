package com.xwab.app.core.audiodelivery.resolution

import com.xwab.app.core.catalog.TrackId

/**
 * What resolving a track produced.
 *
 * A named outcome rather than a nullable source, because the two ways resolution can come back empty
 * mean different things to a listener: a track the catalog no longer holds is a dead end, while a
 * source that could not be reached is worth another tap. Both used to arrive as `null`, and the
 * session dropped it without anything reaching the screen.
 */
sealed interface AudioSourceResolution {
    /**
     * A platform-neutral, non-blank source the playback adapter can hand to the audio engine.
     *
     * It is either an absolute path to an app-owned file or a remote HTTPS URI. Native playback
     * adapters turn that value into their platform URL type.
     */
    data class Resolved(val uri: String) : AudioSourceResolution

    /** The catalog holds no such track. */
    data object NotFound : AudioSourceResolution

    /** The track exists, but nothing playable could be produced for it. */
    data class Unavailable(val reason: String?) : AudioSourceResolution
}

/**
 * Resolves a catalog track into its best currently available playback source.
 *
 * Implementations prefer an already-downloaded file and otherwise answer with the remote HTTPS
 * stream. A cache miss may also start a background download for later playback.
 */
fun interface AudioContentResolver {
    suspend fun resolve(musicId: TrackId): AudioSourceResolution
}
