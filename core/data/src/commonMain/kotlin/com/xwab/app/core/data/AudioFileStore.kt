package com.xwab.app.core.data

/**
 * Platform storage and transport boundary for remotely hosted audio.
 *
 * Implementations must write to a temporary file and only expose a final local URI after the
 * download has completed successfully.
 */
interface AudioFileStore {
    suspend fun find(cacheFileName: String): String?
    suspend fun download(cacheFileName: String, remoteHttpsUrl: String)
}
