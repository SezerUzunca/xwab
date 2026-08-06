package com.xwab.app.core.audiodelivery.resolution

import co.touchlab.kermit.Logger
import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.catalogmanifest.TrackSource
import kotlin.coroutines.cancellation.CancellationException

/**
 * Picks the best source a track can be played from right now: an app-owned local copy when one has
 * been cached, and otherwise the remote HTTPS stream.
 *
 * A cache miss is answered with the stream immediately so playback starts without waiting, and the
 * copy is fetched by [AudioPrefetcher] for later. Everything about *how* that fetch happens —
 * scheduling, retries, its background scope — belongs to the prefetcher, so this class holds no
 * state at all.
 *
 * Which tracks exist is [sourceCatalog]'s answer, not this module's: delivery reads the manifest
 * through that port and never holds a copy of it.
 */
internal class LocalFirstAudioContentResolver(
    private val fileStore: AudioFileStore,
    private val prefetcher: AudioPrefetcher,
    private val sourceCatalog: AudioSourceCatalog,
) : AudioContentResolver {
    private val logger = Logger.withTag("AudioContentResolver")

    override suspend fun resolve(musicId: TrackId): AudioSourceResolution {
        val source = sourceCatalog.sourceFor(musicId) ?: return AudioSourceResolution.NotFound

        return try {
            cachedOrStreamed(source)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Defensive rather than expected: as long as the catalog holds a track it also holds an
            // HTTPS source, so the path below normally cannot come back empty. What this catch is
            // for is where an escaping failure would otherwise *go* — the caller's coroutine, which
            // drops it. A named outcome reaches the session, and the session puts it on the screen.
            logger.e(error) { "Could not resolve a playable source for $musicId." }
            AudioSourceResolution.Unavailable(error.message)
        }
    }

    private suspend fun cachedOrStreamed(source: TrackSource): AudioSourceResolution {
        val cachedUri = fileStore.find(source.cacheFileName)
        if (cachedUri != null) return AudioSourceResolution.Resolved(cachedUri)

        prefetcher.prefetch(source.cacheFileName, source.httpsUrl)
        return AudioSourceResolution.Resolved(source.httpsUrl)
    }
}
