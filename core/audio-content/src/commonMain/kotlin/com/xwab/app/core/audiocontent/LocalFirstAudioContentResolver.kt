package com.xwab.app.core.audiocontent

/**
 * Picks the best source a track can be played from right now: an app-owned local copy when one has
 * been cached, and otherwise the remote HTTPS stream.
 *
 * A cache miss is answered with the stream immediately so playback starts without waiting, and the
 * copy is fetched by [AudioPrefetcher] for later. Everything about *how* that fetch happens —
 * scheduling, retries, its background scope — belongs to the prefetcher, so this class holds no
 * state beyond the catalog index.
 */
internal class LocalFirstAudioContentResolver(
    private val fileStore: AudioFileStore,
    private val prefetcher: AudioPrefetcher,
) : AudioContentResolver {
    private val entriesById = catalogEntries.associateBy { it.music.id }

    override suspend fun resolve(musicId: String): String? {
        val entry = entriesById[musicId] ?: return null
        val cachedUri = fileStore.find(entry.cacheFileName)
        if (cachedUri != null) return cachedUri

        prefetcher.prefetch(entry.cacheFileName, entry.httpsUrl)
        return entry.httpsUrl
    }
}
