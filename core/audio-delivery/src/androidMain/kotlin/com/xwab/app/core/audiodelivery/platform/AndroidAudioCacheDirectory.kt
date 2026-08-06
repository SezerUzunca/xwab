package com.xwab.app.core.audiodelivery.platform

import com.xwab.app.core.audiodelivery.cache.AudioCacheDirectory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [rootDirectory] comes from `noBackupFilesDir`, which is the platform's own answer to "cached
 * bytes that can be downloaded again": Android keeps it out of auto backup without being asked, so
 * unlike the Darwin side there is nothing to set here.
 */
internal class AndroidAudioCacheDirectory(
    private val rootDirectory: File,
) : AudioCacheDirectory {
    override suspend fun prepare() = withContext(Dispatchers.IO) {
        check(rootDirectory.exists() || rootDirectory.mkdirs()) {
            "Could not create the audio content directory."
        }
    }

    override suspend fun sizeOf(fileName: String): Long? = withContext(Dispatchers.IO) {
        val file = File(rootDirectory, fileName)
        if (file.isFile) file.length() else null
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { File(rootDirectory, fileName).delete() }
    }

    override fun localUri(fileName: String): String = File(rootDirectory, fileName).toURI().toString()

    override suspend fun list(): List<String> = withContext(Dispatchers.IO) {
        rootDirectory.list().orEmpty().toList()
    }

    override suspend fun promote(partialFileName: String, cacheFileName: String) {
        withContext(Dispatchers.IO) {
            check(File(rootDirectory, partialFileName).renameTo(File(rootDirectory, cacheFileName))) {
                "Could not promote the completed audio download."
            }
        }
    }
}
