package com.xwab.app.core.audiocontent

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

                // `HttpURLConnection` never follows a redirect across protocols, so an
                // https -> http hop arrives as an unhandled 3xx and is rejected right here.
                // A 4xx is the source's final answer; a 5xx may not be, so only the former is
                // reported as unusable.
                val status = connection.responseCode
                if (status in 400..499) throw UnusableAudioSourceException("Audio source answered HTTP $status.")
                check(status in 200..299) { "Audio download failed with HTTP $status." }

                val contentType = connection.contentType.orEmpty().substringBefore(';')
                if (contentType != "audio/mpeg" && contentType != "application/octet-stream") {
                    throw UnusableAudioSourceException("Unexpected audio content type: $contentType")
                }
                val declaredLength = connection.contentLengthLong
                if (declaredLength > MAX_DOWNLOAD_BYTES) {
                    throw UnusableAudioSourceException("Audio download is too large: $declaredLength bytes.")
                }

                var copied = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            copied += read
                            if (copied > MAX_DOWNLOAD_BYTES) {
                                throw UnusableAudioSourceException("Audio download exceeded the size limit.")
                            }
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
                removeSupersededVersions(cacheFileName)
            } finally {
                connection.disconnect()
                if (partial.exists()) partial.delete()
            }
        }

    private fun removeSupersededVersions(cacheFileName: String) {
        val existing = rootDirectory.list().orEmpty().toList()
        supersededCacheFileNames(existing, cacheFileName).forEach { name ->
            File(rootDirectory, name).delete()
        }
    }

    private fun targetFile(cacheFileName: String): File {
        require(CACHE_FILE_NAME.matches(cacheFileName)) { "Unsafe audio cache file name." }
        return File(rootDirectory, cacheFileName)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_DOWNLOAD_BYTES = 25L * 1024L * 1024L
        const val BUFFER_SIZE = 16 * 1024
        const val USER_AGENT = "SleepSounds/1.0 (audio cache)"
    }
}
