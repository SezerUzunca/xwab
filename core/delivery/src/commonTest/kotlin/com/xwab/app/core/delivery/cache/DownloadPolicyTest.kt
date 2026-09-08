package com.xwab.app.core.delivery.cache

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.network.port.NetworkResponse
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class DownloadPolicyTest {
    private val request = DeliveryRequest(CacheKey("documents", "guide.pdf"), "https://example.test/guide.pdf", setOf("application/pdf"), maxBytes = 10)

    @Test
    fun successfulStatusAndServerFailuresKeepTheirMeaning() {
        requireUsableStatus(200)
        requireUsableStatus(206)
        assertFailsWith<UnusableContentSourceException> { requireUsableStatus(404) }
        assertFailsWith<UnusableContentSourceException> { requireUsableStatus(403) }
        assertFailsWith<IllegalStateException> { requireUsableStatus(500) }
        assertFailsWith<IllegalStateException> { requireUsableStatus(302) }
    }

    @Test
    fun contentTypesAreChosenByTheCaller() {
        requireUsableContentType("Application/PDF; charset=binary", request.acceptedContentTypes)
        requireUsableContentType("Audio/MPEG", setOf("audio/mpeg", "application/octet-stream"))
        requireUsableContentType("application/octet-stream", setOf("audio/mpeg", "application/octet-stream"))
        requireUsableContentType(null, emptySet())
        assertFailsWith<UnusableContentSourceException> { requireUsableContentType("text/html", request.acceptedContentTypes) }
        assertFailsWith<UnusableContentSourceException> { requireUsableContentType(null, request.acceptedContentTypes) }
    }

    @Test
    fun theConfiguredSizeLimitAppliesToDeclaredAndActualBytes() = runBlocking {
        requireWithinSizeLimit(-1, 10)
        requireWithinSizeLimit(10, 10)
        assertFailsWith<UnusableContentSourceException> { requireWithinSizeLimit(11, 10) }
        assertFailsWith<UnusableContentSourceException> {
            FakeNetworkPort(NetworkResponse(200, "application/pdf", 11)).downloadContent(request) { _, _ -> }
        }
        assertFailsWith<UnusableContentSourceException> {
            FakeNetworkPort(NetworkResponse(200, "application/pdf", null), ByteArray(11)).downloadContent(request) { _, _ -> }
        }
        Unit
    }

    @Test
    fun requestPolicyAndBytesPassThroughTheNetworkPort() = runBlocking {
        val network = FakeNetworkPort(NetworkResponse(200, "application/pdf", 3))
        val written = mutableListOf<Byte>()
        network.downloadContent(request) { bytes, count -> written += bytes.take(count) }
        assertEquals("application/pdf", network.headers["Accept"])
        assertContentEquals(byteArrayOf(1, 2, 3), written.toByteArray())
    }

    @Test
    fun callerHeadersReachTheSourceAlongsideTheDerivedAccept() = runBlocking {
        val network = FakeNetworkPort(NetworkResponse(200, "application/pdf", 3))
        network.downloadContent(request.copy(headers = mapOf("User-Agent" to "XWAB/1.0"))) { _, _ -> }
        assertEquals("XWAB/1.0", network.headers["User-Agent"])
        assertEquals("application/pdf", network.headers["Accept"])
    }

    @Test
    fun shortBodiesFailAndUnknownLengthsAreAccepted() = runBlocking {
        assertFailsWith<IllegalStateException> {
            FakeNetworkPort(NetworkResponse(200, "application/pdf", 4)).downloadContent(request) { _, _ -> }
        }
        FakeNetworkPort(NetworkResponse(200, "application/pdf", -1)).downloadContent(request) { _, _ -> }
    }

    private class FakeNetworkPort(private val response: NetworkResponse, private val body: ByteArray = byteArrayOf(1, 2, 3)) : NetworkPort {
        var headers: Map<String, String> = emptyMap()
        override suspend fun getText(httpsUrl: String, headers: Map<String, String>): String = error("unused")
        override suspend fun download(httpsUrl: String, headers: Map<String, String>, onResponse: (NetworkResponse) -> Unit, onChunk: (ByteArray, Int) -> Unit) {
            this.headers = headers
            onResponse(response)
            onChunk(body, body.size)
        }
    }
}
