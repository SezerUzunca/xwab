@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiocontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
// Objective-C categories reach Kotlin as extensions, so these two need importing by name even
// though `NSData` itself is already in scope.
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile

internal class IosAudioFileStore(
    private val rootPath: String,
) : AudioFileStore {
    override suspend fun find(cacheFileName: String): String? = withContext(Dispatchers.Default) {
        val path = targetPath(cacheFileName)
        val data = NSFileManager.defaultManager.contentsAtPath(path)
        if (data != null && data.length > 0uL) {
            NSURL.fileURLWithPath(path).absoluteString
        } else {
            if (data != null) NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            null
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

            val target = targetPath(cacheFileName)
            if (fileManager.contentsAtPath(target)?.length?.let { it > 0uL } == true) {
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
                val remoteUrl = requireNotNull(NSURL.URLWithString(remoteHttpsUrl))
                val data = requireNotNull(NSData.dataWithContentsOfURL(remoteUrl)) {
                    "Could not download remote audio."
                }
                check(data.length > 0uL) { "Downloaded audio is empty." }
                check(data.length <= MAX_DOWNLOAD_BYTES) { "Audio download is too large." }
                check(data.writeToFile(partial, atomically = true)) {
                    "Could not write the temporary audio download."
                }
                check(fileManager.moveItemAtPath(partial, toPath = target, error = null)) {
                    "Could not promote the completed audio download."
                }
            } finally {
                if (fileManager.fileExistsAtPath(partial)) {
                    fileManager.removeItemAtPath(partial, error = null)
                }
            }
        }

    private fun targetPath(cacheFileName: String): String {
        require(CACHE_FILE_PATTERN.matches(cacheFileName)) { "Unsafe audio cache file name." }
        return "$rootPath/$cacheFileName"
    }

    private companion object {
        val CACHE_FILE_PATTERN = Regex("[a-z0-9-]+-v[0-9]+\\.mp3")

        // Not `const`: unsigned arithmetic runs through inline value-class operators, so the
        // product is not a compile-time constant.
        val MAX_DOWNLOAD_BYTES: ULong = 25uL * 1024uL * 1024uL
    }
}
