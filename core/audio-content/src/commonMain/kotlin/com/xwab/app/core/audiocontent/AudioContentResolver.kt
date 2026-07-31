package com.xwab.app.core.audiocontent

/**
 * Resolves a catalog track into its best currently available playback source.
 *
 * Implementations prefer an already-downloaded file, then a bundled resource, and finally a
 * remote HTTPS stream. A remote miss may also start a background download for later playback.
 */
fun interface AudioContentResolver {
    /**
     * A platform-neutral, non-blank URI the playback adapter can hand to the audio engine, or
     * `null` when the catalog holds no such track.
     *
     * It may address a bundled resource, an app-owned local file or a remote HTTPS stream:
     * platform URI types and storage paths deliberately stay behind this boundary.
     */
    suspend fun resolve(musicId: String): String?
}
