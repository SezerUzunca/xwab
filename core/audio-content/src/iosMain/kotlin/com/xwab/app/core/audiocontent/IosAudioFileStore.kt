@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiocontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
// Objective-C categories reach Kotlin as extensions, so the asynchronous convenience method needs
// importing by name even though `NSURLSession` itself is already in scope.
import platform.Foundation.downloadTaskWithRequest

internal class IosAudioFileStore(
    private val rootPath: String,
) : AudioFileStore {
    override suspend fun find(cacheFileName: String): String? = withContext(Dispatchers.Default) {
        val path = targetPath(cacheFileName)
        val size = regularFileSize(path)
        when {
            size == null -> null
            size > 0L -> NSURL.fileURLWithPath(path).absoluteString
            else -> {
                // An empty file is a leftover from a failed write, not a cache hit.
                NSFileManager.defaultManager.removeItemAtPath(path, error = null)
                null
            }
        }
    }

    override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) =
        withContext(Dispatchers.Default) {
            require(remoteHttpsUrl.startsWith("https://")) { "Only HTTPS audio downloads are allowed." }
            val fileManager = NSFileManager.defaultManager
            check(
                fileManager.fileExistsAtPath(rootPath) ||
                    fileManager.createDirectoryAtPath(
                        path = rootPath,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null,
                    ),
            ) { "Could not create the audio content directory." }
            excludeFromBackup()

            val target = targetPath(cacheFileName)
            if ((regularFileSize(target) ?: 0L) > 0L) {
                return@withContext
            }
            if (fileManager.fileExistsAtPath(target)) {
                fileManager.removeItemAtPath(target, error = null)
            }

            val partial = "$rootPath/.$cacheFileName.part"
            if (fileManager.fileExistsAtPath(partial)) {
                fileManager.removeItemAtPath(partial, error = null)
            }
            try {
                val remoteUrl = requireNotNull(NSURL.URLWithString(remoteHttpsUrl)) {
                    "Could not parse the remote audio URL."
                }
                streamToFile(remoteUrl, partial)

                val downloadedBytes = regularFileSize(partial)
                check(downloadedBytes != null && downloadedBytes > 0L) { "Downloaded audio is empty." }
                if (downloadedBytes > MAX_DOWNLOAD_BYTES) {
                    throw UnusableAudioSourceException("Audio download is too large: $downloadedBytes bytes.")
                }
                check(fileManager.moveItemAtPath(partial, toPath = target, error = null)) {
                    "Could not promote the completed audio download."
                }
                removeSupersededVersions(cacheFileName)
            } finally {
                if (fileManager.fileExistsAtPath(partial)) {
                    fileManager.removeItemAtPath(partial, error = null)
                }
            }
        }

    /**
     * Downloads to disk through `NSURLSession`, so a track never has to be held in memory the way
     * `NSData.dataWithContentsOfURL` would, and so the transfer is bounded by a timeout.
     */
    private suspend fun streamToFile(remoteUrl: NSURL, partialPath: String) {
        val configuration = NSURLSessionConfiguration.defaultSessionConfiguration.apply {
            timeoutIntervalForRequest = REQUEST_TIMEOUT_SECONDS
            timeoutIntervalForResource = RESOURCE_TIMEOUT_SECONDS
        }
        val session = NSURLSession.sessionWithConfiguration(configuration)
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = NSMutableURLRequest.requestWithURL(remoteUrl).apply {
                    setValue("audio/mpeg", forHTTPHeaderField = "Accept")
                    setValue(USER_AGENT, forHTTPHeaderField = "User-Agent")
                }
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
        // A 4xx is the source's final answer; a 5xx may not be, so only the former is reported as
        // unusable and spares the caller three attempts at a URL that will never serve audio.
        val status = httpResponse.statusCode
        if (status in 400L..499L) throw UnusableAudioSourceException("Audio source answered HTTP $status.")
        check(status in 200L..299L) { "Audio download failed with HTTP $status." }
        // Unlike Android's `HttpURLConnection`, `NSURLSession` does follow redirects across
        // protocols, so the final URL is worth checking.
        check(httpResponse.URL?.scheme?.equals("https", ignoreCase = true) == true) {
            "Audio download redirected away from HTTPS."
        }

        val contentType = httpResponse.MIMEType.orEmpty().lowercase()
        if (contentType != "audio/mpeg" && contentType != "application/octet-stream") {
            throw UnusableAudioSourceException("Unexpected audio content type: $contentType")
        }
        // `expectedContentLength` is -1 when the server declares no length; the finished file is
        // measured against the same limit once it is on disk.
        if (httpResponse.expectedContentLength > MAX_DOWNLOAD_BYTES) {
            throw UnusableAudioSourceException(
                "Audio download is too large: ${httpResponse.expectedContentLength} bytes.",
            )
        }

        val downloadedPath = checkNotNull(location?.path) { "Audio download produced no file." }
        check(
            NSFileManager.defaultManager.moveItemAtPath(
                downloadedPath,
                toPath = partialPath,
                error = null,
            ),
        ) { "Could not stage the completed audio download." }
    }

    /**
     * Keeps the cache out of the user's iCloud backup, matching what `noBackupFilesDir` gives the
     * Android store for free. Apple's storage guidelines require it: every byte here is a download
     * that can be repeated, and a fully cached catalog is tens of megabytes of someone's quota.
     *
     * Set on the directory, which covers the files inside it, and re-applied on every download so
     * a cache directory created by an older build is corrected too.
     */
    private fun excludeFromBackup() {
        NSURL.fileURLWithPath(rootPath).setResourceValue(
            true,
            forKey = NSURLIsExcludedFromBackupKey,
            error = null,
        )
    }

    private fun removeSupersededVersions(cacheFileName: String) {
        val existing = NSFileManager.defaultManager
            .contentsOfDirectoryAtPath(rootPath, error = null)
            .orEmpty()
            .filterIsInstance<String>()

        supersededCacheFileNames(existing, cacheFileName).forEach { name ->
            NSFileManager.defaultManager.removeItemAtPath("$rootPath/$name", error = null)
        }
    }

    /**
     * Size of the regular file at [path], or `null` when nothing usable is there. Reading the
     * attributes keeps the whole file out of memory, which matters because this runs on every
     * resolve.
     */
    private fun regularFileSize(path: String): Long? {
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
            ?: return null
        if (attributes[NSFileType] != NSFileTypeRegular) return null
        // Foundation boxes the size in an `NSNumber`; the interop bridge may hand it over already
        // unboxed, so both shapes are accepted.
        return when (val size = attributes[NSFileSize]) {
            is NSNumber -> size.longLongValue
            is Number -> size.toLong()
            else -> null
        }
    }

    private fun targetPath(cacheFileName: String): String {
        require(CACHE_FILE_NAME.matches(cacheFileName)) { "Unsafe audio cache file name." }
        return "$rootPath/$cacheFileName"
    }

    private companion object {
        const val MAX_DOWNLOAD_BYTES = 25L * 1024L * 1024L
        const val REQUEST_TIMEOUT_SECONDS = 30.0
        const val RESOURCE_TIMEOUT_SECONDS = 120.0
        const val USER_AGENT = "SleepSounds/1.0 (audio cache)"
    }
}
