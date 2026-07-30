package com.xwab.app.core.data

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidAudioFileStore(
    private val rootDirectory: File,
) : AudioFileStore {
    override suspend fun find(cacheFileName: String): String? = withContext(Dispatchers.IO) {
        val target = targetFile(cacheFileName)
        when {
            !target.isFile -> null
            target.length() > 0L -> target.toURI().toString()
            else -> {
                target.delete()
                null
            }
        }
    }

    override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) =
        withContext(Dispatchers.IO) {
            require(remoteHttpsUrl.startsWith("https://")) { "Only HTTPS audio downloads are allowed." }
            check(rootDirectory.exists() || rootDirectory.mkdirs()) {
                "Could not create the audio content directory."
            }

            val target = targetFile(cacheFileName)
            if (target.isFile && target.length() > 0L) return@withContext
            if (target.exists()) target.delete()

            val partial = File(rootDirectory, ".$cacheFileName.part")
            if (partial.exists()) partial.delete()
            val connection = URL(remoteHttpsUrl).openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "audio/mpeg")
                connection.setRequestProperty("User-Agent", USER_AGENT)

                check(connection.responseCode in 200..299) {
                    "Audio download failed with HTTP ${connection.responseCode}."
                }
                check(connection.url.protocol.equals("https", ignoreCase = true)) {
                    "Audio download redirected away from HTTPS."
                }

                val contentType = connection.contentType.orEmpty().substringBefore(';')
                check(contentType == "audio/mpeg" || contentType == "application/octet-stream") {
                    "Unexpected audio content type: $contentType"
                }
                val declaredLength = connection.contentLengthLong
                check(declaredLength <= MAX_DOWNLOAD_BYTES) { "Audio download is too large." }

                var copied = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            copied += read
                            check(copied <= MAX_DOWNLOAD_BYTES) { "Audio download exceeded the size limit." }
                            output.write(buffer, 0, read)
                        }
                        output.fd.sync()
                    }
                }

                check(copied > 0L) { "Downloaded audio is empty." }
                check(declaredLength < 0L || copied == declaredLength) {
                    "Downloaded audio is incomplete."
                }
                check(partial.renameTo(target)) { "Could not promote the completed audio download." }
            } finally {
                connection.disconnect()
                if (partial.exists()) partial.delete()
            }
        }

    private fun targetFile(cacheFileName: String): File {
        require(CACHE_FILE_PATTERN.matches(cacheFileName)) { "Unsafe audio cache file name." }
        return File(rootDirectory, cacheFileName)
    }

    private companion object {
        val CACHE_FILE_PATTERN = Regex("[a-z0-9-]+-v[0-9]+\\.mp3")
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_DOWNLOAD_BYTES = 25L * 1024L * 1024L
        const val BUFFER_SIZE = 16 * 1024
        const val USER_AGENT = "SleepSounds/1.0 (audio cache)"
    }
}
