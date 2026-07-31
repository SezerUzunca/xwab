package com.xwab.app.core.audiocontent

/**
 * A platform-neutral URI that the playback adapter can hand to the audio engine.
 *
 * The implementation may resolve to a bundled resource, an app-owned local file, or a remote
 * HTTPS stream. Platform URI types and storage paths deliberately stay behind this boundary.
 */
data class ResolvedAudioContent(
    val uri: String,
    val isLocal: Boolean,
) {
    init {
        require(uri.isNotBlank()) { "A resolved audio URI cannot be blank." }
    }
}

/**
 * Resolves a catalog track into its best currently available playback source.
 *
 * Implementations prefer an already-downloaded file, then a bundled resource, and finally a
 * remote HTTPS stream. A remote miss may also start a background download for later playback.
 */
fun interface AudioContentResolver {
    suspend fun resolve(musicId: String): ResolvedAudioContent?
}
