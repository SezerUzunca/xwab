package com.xwab.app.core.audiocontent

import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
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
    private val logger = Logger.withTag("BackgroundAudioPrefetcher")
    private val inFlight = mutableSetOf<String>()
    private val failedAt = mutableMapOf<String, TimeMark>()
    private val stateMutex = Mutex()

    // An atomic start is what makes the `finally` below the only place a slot is handed back: the
    // body is entered even when the scope is already closed, or is closed while this coroutine
    // waits to be dispatched. A plain `launch` would skip both the body and its `finally` in that
    // window, leaving the slot claimed for the lifetime of the prefetcher.
    //
    // What makes the API delicate is the same thing: a body that ignores cancellation would keep
    // working after `close()`. This one does not — the store suspends before it reaches the
    // network, so a canceled transfer unwinds at once and the `finally` is all that runs.
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun prefetch(cacheFileName: String, remoteHttpsUrl: String) {
        if (!claimSlot(cacheFileName)) return

        backgroundScope.launch(start = CoroutineStart.ATOMIC) {
            try {
                downloadWithRetry(cacheFileName, remoteHttpsUrl)
            } finally {
                releaseSlot(cacheFileName)
            }
        }
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
     *
     * Letting a file through also drops its expired mark, which is the only thing that keeps
     * [failedAt] from holding an entry per track that has ever failed.
     */
    private suspend fun claimSlot(cacheFileName: String): Boolean = stateMutex.withLock {
        val coolingDown = failedAt[cacheFileName]?.elapsedNow()?.let { it < FAILURE_COOLDOWN } == true
        if (coolingDown) return@withLock false
        failedAt.remove(cacheFileName)
        inFlight.add(cacheFileName)
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

    /**
     * Starts the cooldown [claimSlot] reads. Runs outside cancellation for the same reason
     * [releaseSlot] does: the mark has to survive a prefetcher that is closing.
     *
     * Only failures are recorded — a success has nothing to write, since claiming the slot already
     * cleared whatever mark the file carried.
     */
    private suspend fun markFailed(cacheFileName: String) {
        withContext(NonCancellable) {
            stateMutex.withLock { failedAt[cacheFileName] = timeSource.markNow() }
        }
    }

    /**
     * A file that is already cached costs nothing here: [AudioFileStore.download] returns without
     * transferring anything, so this does not check the cache first — the resolver has just looked
     * and missed, and a fourth answer to the same question would not be any fresher.
     */
    private suspend fun downloadWithRetry(cacheFileName: String, remoteHttpsUrl: String) {
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                fileStore.download(cacheFileName, remoteHttpsUrl)
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unusable: UnusableAudioSourceException) {
                // The source answered and the answer was wrong; asking twice more only wastes
                // requests and delays the cooldown that keeps the next tap quiet.
                logger.w(unusable) { "Will not cache $cacheFileName; playback will keep using HTTPS." }
                markFailed(cacheFileName)
                return
            } catch (error: Throwable) {
                val isLastAttempt = attempt == DOWNLOAD_ATTEMPTS - 1
                if (isLastAttempt) {
                    logger.w(error) { "Could not cache $cacheFileName; playback will keep using HTTPS." }
                    markFailed(cacheFileName)
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
