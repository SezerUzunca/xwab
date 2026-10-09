package com.xwab.app.core.network

import com.xwab.app.core.network.port.NetworkTransportException
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

/**
 * What MockEngine cannot reproduce: the real OkHttp engine, behind the production client, reading
 * from a local HTTPS server that drops the connection in the middle of the body.
 *
 * Over HTTP/2, which OkHttp negotiates with any server that offers it, the engine can end such a
 * body without an error; the adapter's check of the declared length is what reports it.
 */
class RealEngineInterruptionTest {
    private val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
    private val servers = mutableListOf<MockWebServer>()
    private val engines = mutableListOf<HttpClientEngine>()

    @AfterTest
    fun close() {
        engines.forEach(HttpClientEngine::close)
        servers.forEach(MockWebServer::close)
    }

    @Test
    fun aCompleteBodyArrivesWhole() = runBlocking {
        for (protocols in PROTOCOLS) {
            val server = server(protocols)
            server.enqueue(MockResponse.Builder().body(BODY).build())

            var received = 0
            port().download(server.url("/a").toString(), onResponse = {}, onChunk = { _, count -> received += count })

            assertEquals(BODY.length, received, "over $protocols")
        }
    }

    @Test
    fun aConnectionDroppedMidBodyIsATransportFailure() = runBlocking {
        for (protocols in PROTOCOLS) {
            for (effect in listOf(SocketEffect.CloseSocket(), SocketEffect.ShutdownConnection)) {
                val server = server(protocols)
                server.enqueue(MockResponse.Builder().body(BODY).onResponseBody(effect).build())

                assertFailsWith<NetworkTransportException>("over $protocols with $effect") {
                    port().download(server.url("/a").toString(), onResponse = {}, onChunk = { _, _ -> })
                }
            }
        }
    }

    private fun server(protocols: List<Protocol>) = MockWebServer().apply {
        this.protocols = protocols
        useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory())
        start()
    }.also(servers::add)

    /** The production client with the real engine, trusting only this test's certificate. */
    private fun port() = networkGraphWith(
        OkHttp.create {
            val trusted = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
            preconfigured = OkHttpClient.Builder()
                .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
                .build()
        }.also(engines::add),
    ).network

    private companion object {
        /** Long enough that the server is still sending when the connection drops. */
        val BODY = "x".repeat(200_000)
        val PROTOCOLS = listOf(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1), listOf(Protocol.HTTP_1_1))
    }
}
