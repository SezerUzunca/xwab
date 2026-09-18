@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.playback.platform

import platform.AVFoundation.AVURLAssetHTTPUserAgentKey

/** Local files need no HTTP options; an unconfigured embedding app keeps Apple's default. */
internal fun playbackAssetOptions(scheme: String?, userAgent: String?): Map<Any?, Any>? {
    if (scheme != "https" && scheme != "http") return null
    val identity = userAgent?.takeIf { it.isNotBlank() } ?: return null
    return mapOf(AVURLAssetHTTPUserAgentKey to identity)
}
