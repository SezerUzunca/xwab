package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import com.xwab.app.core.network.port.NetworkTransportException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class KtorNetworkAdapterTest {
    private val clients = mutableListOf<HttpClient>()
    private val engines = mutableListOf<HttpClientEngine>()

    @AfterTest
    fun close() {
        clients.forEach(HttpClient::close)
        engines.forEach(HttpClientEngine::close)
    }

    @Test
    fun downloadsExposeMetadataHeadersAndStreamTheBody() = runBlocking {
        val port = client { request ->
            assertEquals("Xwab", request.headers[HttpHeaders.UserAgent])
            respond("abcdef", headers = headersOf(
                HttpHeaders.ContentType to listOf("audio/mpeg"),
                HttpHeaders.ContentLength to listOf("6"),
            ))
        }
        var metadata: NetworkResponse? = null
        val received = mutableListOf<Byte>()
        port.download(
            "https://example.test/audio.mp3",
            headers = mapOf(HttpHeaders.UserAgent to "Xwab"),
            onResponse = { metadata = it },
            onChunk = { bytes, count -> received += bytes.take(count) },
        )
        assertEquals(200, metadata?.statusCode)
        assertEquals("audio/mpeg", metadata?.contentType)
        assertEquals(6L, metadata?.contentLength)
        assertEquals("abcdef", received.toByteArray().decodeToString())
    }

    @Test
    fun invalidAndCleartextUrlsAreRejectedBeforeTheEngineRuns() = runBlocking {
        val port = client { error("the engine must not be called") }
        for (url in listOf("http://example.test/audio.mp3", "https://example.test:invalid")) {
            assertFailsWith<IllegalArgumentException> {
                port.download(url, onResponse = {}, onChunk = { _, _ -> })
            }
        }
    }

    @Test
    fun nonSuccessResponsesRemainTheCallersPolicy() = runBlocking {
        val port = client { respond("missing", HttpStatusCode.NotFound) }
        var status: Int? = null
        port.download("https://example.test/audio.mp3", onResponse = { status = it.statusCode }, onChunk = { _, _ -> })
        assertEquals(404, status)
    }

    @Test
    fun aRedirectThatDowngradesToCleartextIsNotFollowed() = runBlocking {
        var requests = 0
        val port = client {
            requests++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "http://example.test/audio.mp3"))
        }
        var status: Int? = null
        port.download("https://example.test/audio.mp3", onResponse = { status = it.statusCode }, onChunk = { _, _ -> })
        assertEquals(302, status)
        assertEquals(1, requests)
    }

    @Test
    fun aCleartextUrlReachedByFollowingARedirectIsStillRefused() = runBlocking {
        val port = clientFollowingDowngrades { request ->
            if (request.url.protocol.name == "https") {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "http://example.test/audio.mp3"))
            } else {
                respond("downgraded body")
            }
        }
        assertFailsWith<NetworkTransportException> { port.download() }
        Unit
    }

    @Test
    fun engineFailuresUseThePortException() = runBlocking {
        for (original in listOf(
            IOException("connection failed"),
            ConnectTimeoutException("connect timeout"),
            SocketTimeoutException("socket timeout"),
            HttpRequestTimeoutException("https://example.test/audio.mp3", 1L),
        )) {
            val port = client { throw original }
            val failure = assertFailsWith<NetworkTransportException> { port.download() }
            assertEquals(original::class, failure.cause?.let { it::class })
            assertEquals(original.message, failure.cause?.message)
        }
    }

    @Test
    fun responsePolicyAndSinkFailuresArePropagatedUnchanged() = runBlocking {
        for (failOnResponse in listOf(false, true)) {
            val original = IOException("destination write or policy failed")
            val port = client { respond("abc") }
            val failure = assertFailsWith<IOException> {
                port.download(
                    "https://example.test/audio.mp3",
                    onResponse = { if (failOnResponse) throw original },
                    onChunk = { _, _ -> throw original },
                )
            }
            assertSame(original, failure)
        }
    }

    @Test
    fun callbackCancellationKeepsItsIdentityEvenWithATransportCause() = runBlocking {
        for (failOnResponse in listOf(false, true)) {
            val original = CancellationException("consumer cancelled", IOException("cause"))
            val port = client { respond("abc") }
            val failure = assertFailsWith<CancellationException> {
                port.download(
                    "https://example.test/audio.mp3",
                    onResponse = { if (failOnResponse) throw original },
                    onChunk = { _, _ -> throw original },
                )
            }
            assertSame(original, failure)
        }
    }

    @Test
    fun cancellationWhileWaitingForStreamBytesRemainsCancellation() = runBlocking {
        val port = client { respond(ByteChannel(autoFlush = true)) }
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(30L.milliseconds) { port.download() }
        }
        Unit
    }

    /**
     * The production client's timeouts, as the engine receives them: a connection and a stalled
     * transfer are bounded, the whole download deliberately is not.
     */
    @Test
    fun theClientBoundsConnectingAndStallingButNotTheWholeDownload() = runBlocking {
        var timeouts: HttpTimeoutConfig? = null
        val port = client { request ->
            timeouts = request.getCapabilityOrNull(HttpTimeoutCapability)
            respond("abc")
        }
        port.download()
        assertEquals(10_000L, timeouts?.connectTimeoutMillis)
        assertEquals(30_000L, timeouts?.socketTimeoutMillis)
        assertNull(timeouts?.requestTimeoutMillis)
    }

    private suspend fun NetworkPort.download() =
        download("https://example.test/audio.mp3", onResponse = {}, onChunk = { _, _ -> })

    /** The production graph's client, with only the engine replaced. */
    private fun client(handler: MockRequestHandler): NetworkPort =
        networkGraphWith(MockEngine(handler).also(engines::add)).network

    /**
     * A client the production one could never be: it follows a redirect down to cleartext, which
     * is the only way to reach the adapter's own check of the final URL.
     */
    private fun clientFollowingDowngrades(handler: MockRequestHandler): NetworkPort =
        KtorNetworkAdapter(HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(HttpRedirect) { allowHttpsDowngrade = true }
        }.also(clients::add))
}
