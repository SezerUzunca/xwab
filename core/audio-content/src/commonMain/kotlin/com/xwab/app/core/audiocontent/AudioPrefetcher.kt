package com.xwab.app.core.audiocontent

/**
 * Fills the local audio cache in the background.
 *
 * Kept apart from [LocalFirstAudioContentResolver] because the two answer different questions: the
 * resolver decides which URI is playable *now*, this one decides what is worth fetching for later
 * and how hard to try. Fetching is also the only half of the pair that owns a lifecycle, which is
 * why [close] belongs on this port rather than on the resolver.
 */
internal interface AudioPrefetcher {
    /**
     * Asks for a local copy of [cacheFileName], to be fetched from [remoteHttpsUrl].
     *
     * Returns as soon as the request is registered. The transfer runs in the background and its
     * failures never reach the caller: a missing cache entry only costs a stream, not playback.
     */
    suspend fun prefetch(cacheFileName: String, remoteHttpsUrl: String)

    /** Cancels whatever is still in flight. */
    fun close()
}
