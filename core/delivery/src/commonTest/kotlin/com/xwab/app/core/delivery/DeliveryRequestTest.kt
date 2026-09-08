package com.xwab.app.core.delivery

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DeliveryRequestTest {
    @Test
    fun keysCannotEscapeTheirNamespaceOrOverlapStagedFiles() {
        listOf("../escape", "", "UPPER", "a/b").forEach { namespace ->
            assertFailsWith<IllegalArgumentException> { CacheKey(namespace, "file.bin") }
        }
        listOf("../file", ".file.part", "A.pdf", "file.pdf.part", "a/b.mp3").forEach { file ->
            assertFailsWith<IllegalArgumentException> { CacheKey("documents", file) }
        }
    }

    @Test
    fun requestsCannotRemoveTheirOwnFileOrDisableSizeProtection() {
        val key = CacheKey("documents", "guide.pdf")
        assertFailsWith<IllegalArgumentException> { DeliveryRequest(key, "http://example.test/guide.pdf") }
        assertFailsWith<IllegalArgumentException> { DeliveryRequest(key, "https://example.test/guide.pdf", maxBytes = 0) }
        assertFailsWith<IllegalArgumentException> {
            DeliveryRequest(key, "https://example.test/guide.pdf", retainedFileNames = emptySet())
        }
    }

    @Test
    fun headersCarryCallerIdentityButNeverRestateAccept() {
        val key = CacheKey("documents", "guide.pdf")
        val url = "https://example.test/guide.pdf"
        DeliveryRequest(key, url, headers = mapOf("User-Agent" to "XWAB/1.0"))
        listOf(
            mapOf("Accept" to "application/pdf"),
            mapOf("accept" to "application/pdf"),
            mapOf("" to "value"),
            mapOf("User-Agent" to " "),
        ).forEach { headers ->
            assertFailsWith<IllegalArgumentException> { DeliveryRequest(key, url, headers = headers) }
        }
    }
}
