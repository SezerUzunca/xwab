package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkHttpException
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import com.xwab.app.core.network.port.NetworkTimeoutException
import com.xwab.app.core.network.port.NetworkTransportException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class KtorNetworkAdapterTest {
    private val clients = mutableListOf<HttpClient>()

    @AfterTest
    fun closeClients() {
        clients.forEach(HttpClient::close)
    }

    @Test
    fun textResponsesAreReadThroughKtor() = runBlocking {
        val client = client { request ->
            assertEquals("https://example.test/catalog.json", request.url.toString())
            respond("{\"revision\":2}")
        }

        assertEquals(
            "{\"revision\":2}",
            client.getText("https://example.test/catalog.json"),
        )
    }

    @Test
    fun textRequestsRejectNonSuccessResponses() = runBlocking {
        val client = client { respond("missing", HttpStatusCode.NotFound) }

        val failure = assertFailsWith<NetworkHttpException> {
            client.getText("https://example.test/catalog.json")
        }
        assertEquals(404, failure.statusCode)
    }

    @Test
    fun downloadsExposeMetadataAndStreamTheBody() = runBlocking {
        val client = client {
            respond(
                content = "abcdef",
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("audio/mpeg"),
                    HttpHeaders.ContentLength to listOf("6"),
                ),
            )
        }
        var metadata: NetworkResponse? = null
        val received = mutableListOf<Byte>()

        client.download(
            "https://example.test/audio.mp3",
            onResponse = { metadata = it },
            onChunk = { bytes, count -> received += bytes.take(count) },
        )

        assertEquals(200, metadata?.statusCode)
        assertEquals("audio/mpeg", metadata?.contentType)
        assertEquals(6L, metadata?.contentLength)
        assertEquals("abcdef", received.toByteArray().decodeToString())
    }

    @Test
    fun cleartextIsRejectedBeforeAnEngineRuns() = runBlocking {
        val client = client { error("the engine must not be called") }

        assertFailsWith<IllegalArgumentException> {
            client.getText("http://example.test/catalog.json")
        }
        Unit
    }

    /**
     * A source answering an HTTPS request with a redirect to plain HTTP is asking for the
     * connection to be downgraded. Ktor refuses on its own — `allowHttpsDowngrade` is false by
     * default — so the redirect is never followed and arrives as its own response instead.
     */
    @Test
    fun aRedirectThatDowngradesToCleartextIsNotFollowed() = runBlocking {
        var requests = 0
        val port = client {
            requests++
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "http://example.test/catalog.json"),
            )
        }

        val failure = assertFailsWith<NetworkHttpException> {
            port.getText("https://example.test/catalog.json")
        }

        assertEquals(302, failure.statusCode)
        assertEquals(1, requests)
    }

    /**
     * The port checks the URL that was actually requested, not only the one it was handed. Ktor's
     * own guard is what stops the case above, which leaves the port's guard with nothing to prove
     * it still works — so this lets the downgrade through the client and drives it directly.
     *
     * It surfaces as a transport failure rather than the argument failure an unusable initial URL
     * produces, because the check runs inside the request. Refused either way; only the type
     * differs, and this test is where that choice is written down.
     */
    @Test
    fun aCleartextUrlReachedByFollowingARedirectIsStillRefused() = runBlocking {
        for (download in listOf(false, true)) {
            val port = client(downgradeAllowed = true) { request ->
                if (request.url.protocol.name == "https") {
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "http://example.test/audio.mp3"),
                    )
                } else {
                    respond("the body a downgraded source would have served")
                }
            }

            assertFailsWith<NetworkTransportException> { port.runOperation(download) }
        }
    }

    /**
     * A catalog that hangs must fail as a failure. If it surfaced as a cancellation, every caller
     * that treats cancellation as "my reader went away" — catalog sync does — would neither log
     * it nor back off, and a dead endpoint would be retried on every read forever.
     */
    @Test
    fun aTextRequestThatHangsFailsAsATimeoutAndNotAsACancellation() = runBlocking {
        val client = client(textTimeoutMillis = 30L) {
            delay(Long.MAX_VALUE.milliseconds)
            respond("never arrives")
        }

        val failure = assertFailsWith<NetworkTimeoutException> {
            client.getText("https://example.test/catalog.json")
        }
        assertEquals(30L, failure.timeoutMillis)
    }

    @Test
    fun aCallersTimeoutRemainsCancellation() = runBlocking {
        val client = client(textTimeoutMillis = 10_000L) {
            delay(Long.MAX_VALUE.milliseconds)
            respond("never arrives")
        }

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(30L.milliseconds) {
                client.getText("https://example.test/catalog.json")
            }
        }
        Unit
    }

    @Test
    fun engineFailuresUseTheSamePortExceptionForTextAndDownloads() = runBlocking {
        val failures = listOf(
            IOException("connection failed"),
            ConnectTimeoutException("connect timeout"),
            SocketTimeoutException("socket timeout"),
            HttpRequestTimeoutException("https://example.test/audio.mp3", 1L),
        )
        for (download in listOf(false, true)) {
            for (original in failures) {
                val port = client { throw original }
                val failure = assertFailsWith<NetworkTransportException> {
                    port.runOperation(download)
                }
                // Coroutine stack-trace recovery may copy the engine exception.
                assertEquals(original::class, failure.cause?.let { it::class })
                assertEquals(original.message, failure.cause?.message)
            }
        }
    }

    /**
     * The body ends in a failure rather than in an end of stream, and the bytes written before it
     * still reach the caller.
     *
     * The body is a writer coroutine that emits its bytes and then throws, so the failure is the
     * channel's own closing cause and a reader meets it only after everything written ahead of it.
     * An earlier version wrote to a plain channel and cancelled it from inside `onChunk`, which
     * describes the same scenario but does not produce it: `cancel` is a consumer discarding a
     * stream, and the stream being discarded was this test's own, not the copy the client hands the
     * adapter. Whether the cause crossed that copy turned out to be platform-dependent — it did on
     * Windows and did not on the Linux CI runner, where the adapter read three bytes, saw a clean
     * end of stream and completed normally. That failed three of five CI runs on a commit which
     * passed 13 of 13 locally, and the assertion it broke was the one expecting any failure at all.
     *
     * The writer gets a scope of its own, so the throw it is built around fails the channel rather
     * than the coroutine this test runs in.
     */
    @Test
    fun aFailureAfterAStreamChunkIsStillATransportFailure() = runBlocking {
        val original = IOException("stream disconnected")
        val body = CoroutineScope(Dispatchers.Default).writer {
            channel.writeFully("abc".encodeToByteArray())
            channel.flush()
            throw original
        }.channel
        val port = client { respond(body) }
        var received = 0

        val failure = assertFailsWith<NetworkTransportException> {
            port.download(
                "https://example.test/audio.mp3",
                onResponse = {},
                onChunk = { _, count -> received += count },
            )
        }

        assertEquals(3, received)
        // Identity is the stronger claim, but not one the JVM keeps: coroutine stack-trace
        // recovery copies the exception on its way out, which is what
        // engineFailuresUseTheSamePortExceptionForTextAndDownloads already matches around. It
        // depends on where the exception crosses a coroutine boundary, so it holds until the
        // client's plugin pipeline changes and then stops.
        assertTrue(
            generateSequence(failure.cause) { it.cause }
                .any { it::class == original::class && it.message == original.message },
        )
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
        val channel = ByteChannel(autoFlush = true)
        val port = client { respond(channel) }

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(30L.milliseconds) {
                port.runOperation(download = true)
            }
        }
        Unit
    }

    @Test
    fun urlsRejectedByTheParserUseAStandardArgumentFailure() = runBlocking {
        val port = client { error("the engine must not be called") }
        for (download in listOf(false, true)) {
            assertFailsWith<IllegalArgumentException> {
                if (download) {
                    port.download("https://example.test:invalid", onResponse = {}, onChunk = { _, _ -> })
                } else {
                    port.getText("https://example.test:invalid")
                }
            }
        }
    }

    private suspend fun NetworkPort.runOperation(download: Boolean) {
        if (download) {
            download("https://example.test/audio.mp3", onResponse = {}, onChunk = { _, _ -> })
        } else {
            getText("https://example.test/audio.mp3")
        }
    }

    /**
     * [downgradeAllowed] only ever relaxes Ktor's own guard, so a test can reach the port's. The
     * production client leaves it at Ktor's default.
     */
    private fun client(
        textTimeoutMillis: Long = 15_000L,
        downgradeAllowed: Boolean = false,
        handler: io.ktor.client.engine.mock.MockRequestHandler,
    ): NetworkPort = KtorNetworkAdapter(
        client = HttpClient(MockEngine(handler)) {
            expectSuccess = false
            // Installed only where a test needs Ktor's guard out of the way. Adding a Send phase
            // to every client moves where an exception crosses a coroutine boundary, which was
            // enough to turn the streaming test's cause into a stack-trace-recovery copy.
            if (downgradeAllowed) install(HttpRedirect) { allowHttpsDowngrade = true }
        }.also(clients::add),
        textTimeoutMillis = textTimeoutMillis,
    )
}
