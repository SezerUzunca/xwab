package com.xwab.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class KtorNetworkClientTest {
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
     * A catalog that hangs must fail as a failure. If it surfaced as a cancellation, every caller
     * that treats cancellation as "my reader went away" — catalog sync does — would neither log
     * it nor back off, and a dead endpoint would be retried on every read forever.
     */
    @Test
    fun aTextRequestThatHangsFailsAsATimeoutAndNotAsACancellation() = runBlocking {
        val client = client(textTimeoutMillis = 30L) {
            delay(Long.MAX_VALUE)
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
            delay(Long.MAX_VALUE)
            respond("never arrives")
        }

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(30L) {
                client.getText("https://example.test/catalog.json")
            }
        }
        Unit
    }

    private fun client(
        textTimeoutMillis: Long = 15_000L,
        handler: io.ktor.client.engine.mock.MockRequestHandler,
    ): NetworkClient = KtorNetworkClient(
        client = HttpClient(MockEngine(handler)) { expectSuccess = false },
        textTimeoutMillis = textTimeoutMillis,
    )
}
