@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.playback.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import platform.AVFoundation.AVURLAssetHTTPUserAgentKey

class IosPlaybackSupportTest {
    @Test
    fun streamingUsesTheIdentityConfiguredByTheApplication() {
        val identity = "Sleep/1.0 (https://example.test)"
        for (scheme in listOf("https", "http")) {
            assertEquals(
                mapOf(AVURLAssetHTTPUserAgentKey to identity),
                playbackAssetOptions(scheme, identity),
            )
        }
    }

    @Test
    fun cachedFilesDoNotReceiveHttpOptions() {
        assertNull(playbackAssetOptions("file", "Sleep/1.0"))
        assertNull(playbackAssetOptions(null, "Sleep/1.0"))
    }

    @Test
    fun anEmbeddingAppWithoutAnIdentityKeepsThePlatformDefault() {
        assertNull(playbackAssetOptions("https", null))
        assertNull(playbackAssetOptions("https", ""))
        assertNull(playbackAssetOptions("https", "  "))
    }
}
