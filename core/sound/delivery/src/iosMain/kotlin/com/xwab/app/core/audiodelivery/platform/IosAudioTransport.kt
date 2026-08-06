@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiodelivery.platform

import com.xwab.app.core.audiodelivery.cache.AUDIO_ACCEPT_HEADER
import com.xwab.app.core.audiodelivery.cache.AUDIO_USER_AGENT
import com.xwab.app.core.audiodelivery.cache.AudioTransport
import com.xwab.app.core.audiodelivery.cache.requireUsableContentType
import com.xwab.app.core.audiodelivery.cache.requireUsableStatus
import com.xwab.app.core.audiodelivery.cache.requireWithinSizeLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
// Objective-C categories reach Kotlin as extensions, so these two need importing by name even
// though the classes that answer them are already in scope: `downloadTaskWithRequest:` lives in
// NSURLSession's asynchronous convenience category, and the header setter in
// NSMutableURLRequest's NSMutableHTTPURLRequest category.
import platform.Foundation.downloadTaskWithRequest
import platform.Foundation.setValue
import platform.posix.O_RDONLY
import platform.posix.close
import platform.posix.fsync
import platform.posix.open

internal class IosAudioTransport(
    private val rootPath: String,
) : AudioTransport {
    /**
     * One session for the life of the transport, as Apple asks. A session per download built its
     * own connection pool and threw away the TLS handshake, which the prefetcher pays for on every
     * track it fills the cache with.
     */
    private val session by lazy {
        NSURLSession.sessionWithConfiguration(
            NSURLSessionConfiguration.defaultSessionConfiguration.apply {
                timeoutIntervalForRequest = REQUEST_TIMEOUT_SECONDS
                timeoutIntervalForResource = RESOURCE_TIMEOUT_SECONDS
            },
        )
    }

    override suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String) {
        withContext(Dispatchers.Default) {
            val remoteUrl = requireNotNull(NSURL.URLWithString(remoteHttpsUrl)) {
                "Could not parse the remote audio URL."
            }
            val partialPath = "$rootPath/$partialFileName"
            streamToFile(remoteUrl, partialPath)
            flushToDisk(partialPath)
        }
    }

    /**
     * Matches the `FileDescriptor.sync()` the Android transport ends on. The promotion that follows
     * is a rename, which orders nothing by itself: without this a crash could leave a file that is
     * truncated but not empty under the final name — and a non-empty file is a cache hit, so it
     * would be served as audio and never re-fetched.
     */
    private fun flushToDisk(path: String) {
        val descriptor = open(path, O_RDONLY)
        if (descriptor < 0) return
        try {
            fsync(descriptor)
        } finally {
            close(descriptor)
        }
    }

    /**
     * Downloads to disk through `NSURLSession`, so a track never has to be held in memory the way
     * `NSData.dataWithContentsOfURL` would, and so the transfer is bounded by a timeout.
     *
     * The session's convenience API reports only the finished body, so unlike the Android transport
     * this cannot abort an oversized transfer while it is arriving — the size limit is applied to
     * what the source declares, and again by the store to what actually landed. Until that is
     * closed, `RESOURCE_TIMEOUT_SECONDS` is the real ceiling on a source that declares nothing.
     */
    private suspend fun streamToFile(remoteUrl: NSURL, partialPath: String) {
        suspendCancellableCoroutine<Unit> { continuation ->
            // `requestWithURL:` is declared on `NSURLRequest`, so interop types the result as
            // the immutable class even though the mutable subclass is what answers the
            // message. Only the static type is wrong; the instance really is mutable.
            val request = (NSMutableURLRequest.requestWithURL(remoteUrl) as NSMutableURLRequest)
            request.setValue(AUDIO_ACCEPT_HEADER, forHTTPHeaderField = "Accept")
            request.setValue(AUDIO_USER_AGENT, forHTTPHeaderField = "User-Agent")
            val task = session.downloadTaskWithRequest(request) { location, response, error ->
                // The system deletes `location` as soon as this handler returns, so the file
                // has to be claimed here rather than after the coroutine resumes.
                continuation.resumeWith(
                    runCatching { claimDownload(location, response, error, partialPath) },
                )
            }
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        }
    }

    private fun claimDownload(
        location: NSURL?,
        response: NSURLResponse?,
        error: NSError?,
        partialPath: String,
    ) {
        check(error == null) { "Audio download failed: ${error?.localizedDescription}" }

        val httpResponse = checkNotNull(response as? NSHTTPURLResponse) {
            "Audio download returned a non-HTTP response."
        }
        requireUsableStatus(httpResponse.statusCode.toInt())
        // Unlike Android's `HttpURLConnection`, `NSURLSession` does follow redirects across
        // protocols, so the final URL is worth checking.
        check(httpResponse.URL?.scheme?.equals("https", ignoreCase = true) == true) {
            "Audio download redirected away from HTTPS."
        }
        requireUsableContentType(httpResponse.MIMEType)
        // `expectedContentLength` is -1 when the server declares no length; the finished file is
        // measured against the same limit once it is on disk.
        requireWithinSizeLimit(httpResponse.expectedContentLength)

        val downloadedPath = checkNotNull(location?.path) { "Audio download produced no file." }
        check(
            NSFileManager.defaultManager.moveItemAtPath(
                downloadedPath,
                toPath = partialPath,
                error = null,
            ),
        ) { "Could not stage the completed audio download." }
    }

    private companion object {
        const val REQUEST_TIMEOUT_SECONDS = 30.0
        const val RESOURCE_TIMEOUT_SECONDS = 120.0
    }
}
