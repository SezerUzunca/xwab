package com.xwab.app.core.delivery.resolution

import com.xwab.app.core.delivery.cache.ContentFileStore
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.delivery.port.DeliveryResult
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

class LocalFirstDeliveryAdapterTest {
    private val request = DeliveryRequest(CacheKey("documents", "guide-v1.pdf"), "https://example.test/guide.pdf")

    @Test
    fun uncachedContentReturnsHttpsAndStartsPrefetch() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val adapter = LocalFirstDeliveryAdapter(FakeContentFileStore(), prefetcher)
        assertEquals(request.httpsUrl, assertIs<DeliveryResult.Resolved>(adapter.resolve(request)).uri)
        assertEquals(listOf(request), prefetcher.requests)
    }

    @Test
    fun cachedContentReturnsItsPathWithoutPrefetch() = runBlocking {
        val prefetcher = RecordingPrefetcher()
        val adapter = LocalFirstDeliveryAdapter(FakeContentFileStore("/content/documents/guide-v1.pdf"), prefetcher)
        assertEquals("/content/documents/guide-v1.pdf", assertIs<DeliveryResult.Resolved>(adapter.resolve(request)).uri)
        assertTrue(prefetcher.requests.isEmpty())
    }

    @Test
    fun cacheFailuresFallBackToHttps() = runBlocking {
        val adapter = LocalFirstDeliveryAdapter(FakeContentFileStore(failure = IllegalStateException("unreadable")), RecordingPrefetcher())
        assertEquals(request.httpsUrl, assertIs<DeliveryResult.Resolved>(adapter.resolve(request)).uri)
    }

    @Test
    fun prefetchFailuresDoNotPreventStreaming() = runBlocking {
        val adapter = LocalFirstDeliveryAdapter(FakeContentFileStore(), RecordingPrefetcher(IllegalStateException("closed")))
        assertEquals(request.httpsUrl, assertIs<DeliveryResult.Resolved>(adapter.resolve(request)).uri)
    }

    @Test
    fun prefetchCancellationStillPropagates() = runBlocking {
        val adapter = LocalFirstDeliveryAdapter(FakeContentFileStore(), RecordingPrefetcher(CancellationException()))
        assertFailsWith<CancellationException> { adapter.resolve(request) }
        Unit
    }

    @Test
    fun cancellationStillPropagates() = runBlocking {
        val adapter = LocalFirstDeliveryAdapter(FakeContentFileStore(failure = CancellationException()), RecordingPrefetcher())
        assertFailsWith<CancellationException> { adapter.resolve(request) }
        Unit
    }

    private class FakeContentFileStore(private val path: String? = null, private val failure: Throwable? = null) : ContentFileStore {
        override suspend fun find(key: CacheKey): String? { failure?.let { throw it }; return path }
        override suspend fun download(request: DeliveryRequest): Unit = fail("Only the prefetcher downloads.")
    }

    private class RecordingPrefetcher(private val failure: Exception? = null) : ContentPrefetcher {
        val requests = mutableListOf<DeliveryRequest>()
        override suspend fun prefetch(request: DeliveryRequest) { failure?.let { throw it }; requests += request }
        override fun close() = Unit
    }
}
