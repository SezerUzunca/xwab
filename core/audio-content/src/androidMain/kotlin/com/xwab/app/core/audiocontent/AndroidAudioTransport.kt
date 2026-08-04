package com.xwab.app.core.audiocontent

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidAudioTransport(
    private val rootDirectory: File,
) : AudioTransport {
    override suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String) {
        withContext(Dispatchers.IO) {
            val partial = File(rootDirectory, partialFileName)
            val connection = URL(remoteHttpsUrl).openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", AUDIO_ACCEPT_HEADER)
                connection.setRequestProperty("User-Agent", AUDIO_USER_AGENT)

                // `HttpURLConnection` never follows a redirect across protocols, so an
                // https -> http hop arrives as an unhandled 3xx and is rejected by the status
                // rule. The Darwin transport has to check the final URL itself, since its session
                // does follow such a hop.
                requireUsableStatus(connection.responseCode)
                requireUsableContentType(connection.contentType)
                val declaredLength = connection.contentLengthLong
                requireWithinSizeLimit(declaredLength)

                var copied = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            copied += read
                            // A source that declared no length is bounded here instead, before the
                            // bytes are on disk rather than after.
                            requireWithinSizeLimit(copied)
                            output.write(buffer, 0, read)
                        }
                        // The promotion that follows is a rename, which orders nothing by itself:
                        // without this a crash could leave a truncated file under the final name.
                        output.fd.sync()
                    }
                }

                check(declaredLength < 0L || copied == declaredLength) {
                    "Downloaded audio is incomplete."
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 16 * 1024
    }
}
