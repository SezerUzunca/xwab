package com.xwab.app.core.audiodelivery.cache

import kotlin.test.Test
import kotlin.test.assertFailsWith

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
}
