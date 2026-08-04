package com.xwab.app.core.audiocontent.cache

/**
 * The ceiling a cached track may not cross. Generous for a sleep sound and small enough that a
 * source answering with something other than audio cannot fill a phone before it is refused.
 */
internal const val MAX_DOWNLOAD_BYTES = 25L * 1024L * 1024L

internal const val AUDIO_ACCEPT_HEADER = "audio/mpeg"

internal const val AUDIO_USER_AGENT = "SleepSounds/1.0 (audio cache)"

/**
 * A 4xx is the source's final answer, so it is raised as unusable and spares the caller two more
 * attempts at a URL that will never serve audio. A 5xx may well be temporary, so it stays an
 * ordinary failure and keeps its retries.
 */
internal fun requireUsableStatus(status: Int) {
    if (status in 400..499) throw UnusableAudioSourceException("Audio source answered HTTP $status.")
    check(status in 200..299) { "Audio download failed with HTTP $status." }
}

/**
 * Media types are case-insensitive and may arrive with parameters and surrounding space, so the
 * header is reduced to its bare type before it is compared. `application/octet-stream` is accepted
 * because hosts that serve audio out of generic storage answer with it.
 *
 * This rule used to live in both stores, and that is exactly how it drifted: the Darwin side
 * lowercased the header and the Android side did not, so a host answering `Audio/MPEG` was cached
 * on one platform and permanently refused on the other.
 */
internal fun requireUsableContentType(rawContentType: String?) {
    val contentType = rawContentType.orEmpty().substringBefore(';').trim().lowercase()
    if (contentType != "audio/mpeg" && contentType != "application/octet-stream") {
        throw UnusableAudioSourceException("Unexpected audio content type: $contentType")
    }
}

/**
 * Holds both the length a source declares and the bytes actually written against the same limit.
 * A negative [bytes] is a source that declared no length, which is not by itself a refusal — the
 * transfer is measured again as it lands.
 */
internal fun requireWithinSizeLimit(bytes: Long) {
    if (bytes > MAX_DOWNLOAD_BYTES) {
        throw UnusableAudioSourceException("Audio download is too large: $bytes bytes.")
    }
}
