package com.xwab.app.core.audiodelivery.cache

import com.xwab.app.core.network.NetworkClient
import com.xwab.app.core.network.NetworkResponse
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

/**
 * The policy decides what is worth a retry and what is a dead end, so these cases are the contract
 * both transports answer to — and the reason the rules stopped being written twice.
 */
class AudioDownloadPolicyTest {
    @Test
    fun aSuccessfulStatusIsAccepted() {
        requireUsableStatus(200)
        requireUsableStatus(206)
    }

    /** A client error will answer the same way tomorrow, so the attempts are spent for nothing. */
    @Test
    fun aClientErrorIsUnusableRatherThanRetried() {
        assertFailsWith<UnusableAudioSourceException> { requireUsableStatus(404) }
        assertFailsWith<UnusableAudioSourceException> { requireUsableStatus(403) }
    }

    @Test
    fun aServerErrorOrAnUnhandledRedirectKeepsItsRetries() {
        assertFailsWith<IllegalStateException> { requireUsableStatus(500) }
        assertFailsWith<IllegalStateException> { requireUsableStatus(302) }
    }

    /**
     * Media types are case-insensitive and carry parameters, which is precisely where the two
     * hand-written copies of this rule disagreed.
     */
    @Test
    fun aContentTypeIsComparedWithoutCaseParametersOrSpace() {
        requireUsableContentType("audio/mpeg")
        requireUsableContentType("Audio/MPEG")
        requireUsableContentType("audio/mpeg; charset=utf-8")
        requireUsableContentType("audio/mpeg ; charset=utf-8")
        requireUsableContentType("application/octet-stream")
    }

    @Test
    fun anythingThatIsNotAudioIsUnusable() {
        assertFailsWith<UnusableAudioSourceException> { requireUsableContentType("text/html") }
        assertFailsWith<UnusableAudioSourceException> { requireUsableContentType(null) }
    }

    @Test
    fun anUndeclaredLengthPassesAndTheLimitHolds() {
        requireWithinSizeLimit(-1L)
        requireWithinSizeLimit(MAX_DOWNLOAD_BYTES)
        assertFailsWith<UnusableAudioSourceException> { requireWithinSizeLimit(MAX_DOWNLOAD_BYTES + 1) }
    }

    @Test
    fun audioHeadersAndResponseChecksWrapTheNetworkStreamOnce() = runBlocking {
        val network = FakeNetworkClient(
            response = NetworkResponse(200, "Audio/MPEG; charset=binary", 3),
            body = byteArrayOf(1, 2, 3),
        )
        val written = mutableListOf<Byte>()

        network.downloadAudio("https://example.test/audio.mp3") { bytes, count ->
            written += bytes.take(count)
        }

        assertEquals(AUDIO_ACCEPT_HEADER, network.headers["Accept"])
        assertEquals(AUDIO_USER_AGENT, network.headers["User-Agent"])
        assertContentEquals(byteArrayOf(1, 2, 3), written.toByteArray())
    }

    @Test
    fun aShortNetworkBodyIsRefusedBeforeThePlatformPromotesIt() = runBlocking {
        val network = FakeNetworkClient(
            response = NetworkResponse(200, "audio/mpeg", 4),
            body = byteArrayOf(1, 2, 3),
        )

        assertFailsWith<IllegalStateException> {
            network.downloadAudio("https://example.test/audio.mp3") { _, _ -> }
        }
        Unit
    }

    private class FakeNetworkClient(
        private val response: NetworkResponse,
        private val body: ByteArray,
    ) : NetworkClient {
        var headers: Map<String, String> = emptyMap()

        override suspend fun getText(httpsUrl: String, headers: Map<String, String>): String =
            error("not used")

        override suspend fun download(
            httpsUrl: String,
            headers: Map<String, String>,
            onResponse: (NetworkResponse) -> Unit,
            onChunk: (bytes: ByteArray, count: Int) -> Unit,
        ) {
            this.headers = headers
            onResponse(response)
            onChunk(body, body.size)
        }
    }
}
