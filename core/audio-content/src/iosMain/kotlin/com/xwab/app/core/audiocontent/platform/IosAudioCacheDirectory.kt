@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.audiocontent.platform

import com.xwab.app.core.audiocontent.cache.AudioCacheDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

internal class IosAudioCacheDirectory(
    private val rootPath: String,
) : AudioCacheDirectory {
    override suspend fun prepare() = withContext(Dispatchers.Default) {
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
    }

    override suspend fun sizeOf(fileName: String): Long? = withContext(Dispatchers.Default) {
        regularFileSize(pathOf(fileName))
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.Default) {
            NSFileManager.defaultManager.removeItemAtPath(pathOf(fileName), error = null)
        }
    }

    // `absoluteString` is nullable across interop but a file URL always has one, and answering
    // `null` here would read as a cache miss for a file that is sitting right there.
    override fun localUri(fileName: String): String =
        requireNotNull(NSURL.fileURLWithPath(pathOf(fileName)).absoluteString) {
            "Could not form a local URI for $fileName."
        }

    override suspend fun list(): List<String> = withContext(Dispatchers.Default) {
        NSFileManager.defaultManager
            .contentsOfDirectoryAtPath(rootPath, error = null)
            .orEmpty()
            .filterIsInstance<String>()
    }

    override suspend fun promote(partialFileName: String, cacheFileName: String) {
        withContext(Dispatchers.Default) {
            // Unlike POSIX `rename`, this fails rather than replaces when the destination exists.
            // The store clears the final name before the transfer starts, and single-flight keeps
            // anything else from putting it back in the meantime.
            check(
                NSFileManager.defaultManager.moveItemAtPath(
                    pathOf(partialFileName),
                    toPath = pathOf(cacheFileName),
                    error = null,
                ),
            ) { "Could not promote the completed audio download." }
        }
    }

    /**
     * Keeps the cache out of the user's iCloud backup, matching what `noBackupFilesDir` gives the
     * Android side for free. Apple's storage guidelines require it: every byte here is a download
     * that can be repeated, and a fully cached catalog is tens of megabytes of someone's quota.
     *
     * Set on the directory, which covers the files inside it, and re-applied on every prepare so a
     * cache directory created by an older build is corrected too.
     */
    private fun excludeFromBackup() {
        NSURL.fileURLWithPath(rootPath).setResourceValue(
            true,
            forKey = NSURLIsExcludedFromBackupKey,
            error = null,
        )
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

    private fun pathOf(fileName: String): String = "$rootPath/$fileName"
}
