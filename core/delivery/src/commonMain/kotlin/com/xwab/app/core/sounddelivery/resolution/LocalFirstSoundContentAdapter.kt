package com.xwab.app.core.sounddelivery.resolution

import co.touchlab.kermit.Logger
import com.xwab.app.core.sounddelivery.cache.AudioFileStore
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.sounddelivery.port.SoundContentResolution
import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sound.port.TrackSource
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
 * Which tracks exist is [sourcePort]'s answer, not this module's: delivery reads the manifest
 * through that port and never holds a copy of it.
 */
internal class LocalFirstSoundContentAdapter(
    private val fileStore: AudioFileStore,
    private val prefetcher: AudioPrefetcher,
    private val sourcePort: SoundPort,
) : SoundContentPort {
    private val logger = Logger.withTag("SoundContentPort")

    override suspend fun resolve(trackId: TrackId): SoundContentResolution {
        val source = sourcePort.sourceFor(trackId) ?: return SoundContentResolution.NotFound

        return try {
            cachedOrStreamed(source)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Defensive rather than expected: as long as the catalog holds a track it also holds an
            // HTTPS source, so the path below normally cannot come back empty. What this catch is
            // for is where an escaping failure would otherwise *go* — the caller's coroutine, which
            // drops it. A named outcome reaches the session, and the session puts it on the screen.
            logger.e(error) { "Could not resolve a playable source for $trackId." }
            SoundContentResolution.Unavailable(error.message)
        }
    }

    private suspend fun cachedOrStreamed(source: TrackSource): SoundContentResolution {
        val cachedPath = fileStore.find(source.cacheFileName)
        if (cachedPath != null) return SoundContentResolution.Resolved(cachedPath)

        prefetcher.prefetch(source.cacheFileName, source.httpsUrl)
        return SoundContentResolution.Resolved(source.httpsUrl)
    }
}
