package com.xwab.app.core.audiocontent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Download policy only: what gets fetched, how often, and what happens when the network refuses.
 */
class BackgroundAudioPrefetcherTest {
    @Test
    fun aRequestedFileIsDownloadedOnce() = runBlocking {
        val fileStore = FakeAudioFileStore()
        val prefetcher = BackgroundAudioPrefetcher(fileStore)
        try {
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)

            withTimeout(TIMEOUT_MS) { fileStore.downloaded.await() }
            assertEquals(1, fileStore.downloadCount)
        } finally {
            prefetcher.close()
        }
    }

    @Test
    fun repeatedRequestsShareOneInFlightDownload() = runBlocking {
        val fileStore = BlockingAudioFileStore()
        val prefetcher = BackgroundAudioPrefetcher(fileStore)
        try {
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            withTimeout(TIMEOUT_MS) { fileStore.downloadStarted.await() }

            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            assertEquals(1, fileStore.downloadCount)

            fileStore.releaseDownload.complete(Unit)
            withTimeout(TIMEOUT_MS) { fileStore.downloadFinished.await() }
            assertEquals(1, fileStore.downloadCount)
        } finally {
            prefetcher.close()
        }
    }

    /**
     * The caller is never told: a track that cannot be cached still plays from HTTPS, so a failed
     * prefetch is a log line and nothing else.
     */
    @Test
    fun aFailingDownloadIsRetriedThreeTimesAndThenGivenUpQuietly() = runBlocking {
        val fileStore = FailingAudioFileStore()
        val prefetcher = BackgroundAudioPrefetcher(fileStore)
        try {
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)

            withTimeout(RETRY_TIMEOUT_MS) { fileStore.attemptsExhausted.await() }
            assertEquals(3, fileStore.attempts)
        } finally {
            prefetcher.close()
        }
    }

    /**
     * The whole point of the cooldown: browsing the catalog while offline must not restart a
     * three-attempt burst on every tap.
     */
    @Test
    fun aFileThatJustFailedIsNotRetriedAgainImmediately() = runBlocking {
        val fileStore = FailingAudioFileStore()
        val scope = testScope()
        val prefetcher = BackgroundAudioPrefetcher(fileStore, scope)
        try {
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            scope.settle()
            assertEquals(3, fileStore.attempts)

            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            scope.settle()

            assertEquals(3, fileStore.attempts, "the cooldown should have refused a second burst")
        } finally {
            prefetcher.close()
        }
    }

    @Test
    fun aFileIsRetriedOnceTheCooldownHasPassed() = runBlocking {
        val fileStore = FailingAudioFileStore()
        val time = FakeTimeSource()
        val scope = testScope()
        val prefetcher = BackgroundAudioPrefetcher(fileStore, scope, time)
        try {
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            scope.settle()

            time.advance(10.minutes)
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            scope.settle()

            assertEquals(6, fileStore.attempts, "a second burst should follow the cooldown")
        } finally {
            prefetcher.close()
        }
    }

    /**
     * A source that answers with the wrong thing will answer the same way three times, so the
     * attempts are spent for nothing — and the backoff would delay the cooldown that follows.
     */
    @Test
    fun anUnusableSourceIsNotRetriedAtAll() = runBlocking {
        val fileStore = UnusableAudioFileStore()
        val scope = testScope()
        val prefetcher = BackgroundAudioPrefetcher(fileStore, scope)
        try {
            prefetcher.prefetch(FILE_NAME, REMOTE_URL)
            scope.settle()

            assertEquals(1, fileStore.attempts, "an unusable source should not be retried")
        } finally {
            prefetcher.close()
        }
    }

    private fun testScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Waits for the whole attempt cycle, including the slot release in its `finally`.
     *
     * The store's own signals fire *inside* an attempt, so asserting on them would race the
     * bookkeeping that runs after it — which is exactly what a cooldown test must not do.
     */
    private suspend fun CoroutineScope.settle() {
        this.coroutineContext.job.children.toList().forEach { it.join() }
    }

    private class FakeAudioFileStore : AudioFileStore {
        val downloaded = CompletableDeferred<Unit>()
        var downloadCount = 0

        override suspend fun find(cacheFileName: String): String? = null

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
            downloadCount++
            downloaded.complete(Unit)
        }
    }

    private class BlockingAudioFileStore : AudioFileStore {
        val downloadStarted = CompletableDeferred<Unit>()
        val releaseDownload = CompletableDeferred<Unit>()
        val downloadFinished = CompletableDeferred<Unit>()
        var downloadCount = 0

        override suspend fun find(cacheFileName: String): String? = null

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
            downloadCount++
            downloadStarted.complete(Unit)
            releaseDownload.await()
            downloadFinished.complete(Unit)
        }
    }

    private class FailingAudioFileStore : AudioFileStore {
        val attemptsExhausted = CompletableDeferred<Unit>()
        var attempts = 0

        override suspend fun find(cacheFileName: String): String? = null

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
            attempts++
            if (attempts == 3) attemptsExhausted.complete(Unit)
            error("The audio host is unreachable.")
        }
    }

    private class UnusableAudioFileStore : AudioFileStore {
        var attempts = 0

        override suspend fun find(cacheFileName: String): String? = null

        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
            attempts++
            throw UnusableAudioSourceException("Unexpected audio content type: text/html")
        }
    }

    /** Enough of a [TimeSource] to move past the failure cooldown without waiting for it. */
    private class FakeTimeSource : TimeSource {
        private var now: Duration = Duration.ZERO

        fun advance(by: Duration) {
            now += by
        }

        override fun markNow(): TimeMark {
            val markedAt = now
            return object : TimeMark {
                override fun elapsedNow(): Duration = now - markedAt
            }
        }
    }

    private companion object {
        const val FILE_NAME = "heavy-rain-v1.mp3"
        const val REMOTE_URL = "https://example.test/heavy-rain.mp3"
        const val TIMEOUT_MS = 2_000L

        // Two backoff waits (500 ms + 1000 ms) sit between the three attempts.
        const val RETRY_TIMEOUT_MS = 5_000L
    }
}
