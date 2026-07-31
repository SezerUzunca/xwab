package com.xwab.app.core.audiocontent

import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single-flight downloads on an owned background scope, with a bounded retry.
 *
 * The in-flight set is keyed by cache file name, which already carries the track id and its
 * version, so two requests for the same file share one transfer while a version bump gets its own.
 */
internal class BackgroundAudioPrefetcher(
    private val fileStore: AudioFileStore,
    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : AudioPrefetcher {
    private val logger = Logger.withTag("AudioContent")
    private val inFlight = mutableSetOf<String>()
    private val failedAt = mutableMapOf<String, TimeMark>()
    private val stateMutex = Mutex()

    override suspend fun prefetch(cacheFileName: String, remoteHttpsUrl: String) {
        if (!claimSlot(cacheFileName)) return

        val download = backgroundScope.launch {
            try {
                downloadWithRetry(cacheFileName, remoteHttpsUrl)
            } finally {
                releaseSlot(cacheFileName)
            }
        }
        // `launch` on a closed prefetcher's scope never starts the body, so the `finally` above
        // never runs either. The slot has to be handed back here instead.
        if (download.isCancelled) releaseSlot(cacheFileName)
    }

    override fun close() {
        backgroundScope.cancel()
    }

    /**
     * `true` when this call took the slot and therefore owns the transfer.
     *
     * A file that has just exhausted its attempts is refused until [FAILURE_COOLDOWN] passes.
     * Without that, browsing the catalog while offline would start a fresh three-attempt burst on
     * every tap, since a finished failure leaves nothing behind but a log line.
     */
    private suspend fun claimSlot(cacheFileName: String): Boolean = stateMutex.withLock {
        val coolingDown = failedAt[cacheFileName]?.elapsedNow()?.let { it < FAILURE_COOLDOWN } == true
        if (coolingDown) false else inFlight.add(cacheFileName)
    }

    /**
     * Runs outside cancellation on purpose: `withLock` suspends, so a canceled download would
     * otherwise skip the release and keep its slot for good — that file could then never be
     * queued again for the lifetime of the prefetcher.
     */
    private suspend fun releaseSlot(cacheFileName: String) {
        withContext(NonCancellable) {
            stateMutex.withLock { inFlight.remove(cacheFileName) }
        }
    }

    /** Records the cache result and marks failed downloads for the cooldown period. */
    private suspend fun recordOutcome(cacheFileName: String, cached: Boolean) {
        withContext(NonCancellable) {
            stateMutex.withLock {
                if (cached) failedAt.remove(cacheFileName) else failedAt[cacheFileName] = timeSource.markNow()
            }
        }
    }

    private suspend fun downloadWithRetry(cacheFileName: String, remoteHttpsUrl: String) {
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                if (fileStore.find(cacheFileName) == null) {
                    fileStore.download(cacheFileName, remoteHttpsUrl)
                }
                recordOutcome(cacheFileName, cached = true)
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unusable: UnusableAudioSourceException) {
                // The source answered and the answer was wrong; asking twice more only wastes
                // requests and delays the cooldown that keeps the next tap quiet.
                logger.w(unusable) { "Will not cache $cacheFileName; playback will keep using HTTPS." }
                recordOutcome(cacheFileName, cached = false)
                return
            } catch (error: Throwable) {
                val isLastAttempt = attempt == DOWNLOAD_ATTEMPTS - 1
                if (isLastAttempt) {
                    logger.w(error) { "Could not cache $cacheFileName; playback will keep using HTTPS." }
                    recordOutcome(cacheFileName, cached = false)
                } else {
                    delay(RETRY_BASE_DELAY * (1 shl attempt))
                }
            }
        }
    }

    private companion object {
        const val DOWNLOAD_ATTEMPTS = 3
        val RETRY_BASE_DELAY = 500.milliseconds
        val FAILURE_COOLDOWN = 5.minutes
    }
}
