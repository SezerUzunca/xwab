@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiocontent

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

internal class IosAudioTransport(
    private val rootPath: String,
) : AudioTransport {
    override suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String) {
        withContext(Dispatchers.Default) {
            val remoteUrl = requireNotNull(NSURL.URLWithString(remoteHttpsUrl)) {
                "Could not parse the remote audio URL."
            }
            streamToFile(remoteUrl, "$rootPath/$partialFileName")
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
        val configuration = NSURLSessionConfiguration.defaultSessionConfiguration.apply {
            timeoutIntervalForRequest = REQUEST_TIMEOUT_SECONDS
            timeoutIntervalForResource = RESOURCE_TIMEOUT_SECONDS
        }
        val session = NSURLSession.sessionWithConfiguration(configuration)
        try {
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
        } finally {
            session.finishTasksAndInvalidate()
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
