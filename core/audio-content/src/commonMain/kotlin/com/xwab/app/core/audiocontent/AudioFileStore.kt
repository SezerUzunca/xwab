package com.xwab.app.core.audiocontent

/**
 * Platform storage and transport boundary for remotely hosted audio.
 *
 * Implementations must write to a temporary file and only expose a final local URI after the
 * download has completed successfully. A completed download also retires the copies it supersedes,
 * so a track never keeps more than one version on disk.
 */
interface AudioFileStore {
    suspend fun find(cacheFileName: String): String?
    suspend fun download(cacheFileName: String, remoteHttpsUrl: String)
}

/**
 * A download that trying again cannot fix: the source answered, and the answer was unusable — the
 * wrong status, the wrong content type, or more bytes than the cache accepts.
 *
 * Stores raise this instead of a plain failure so the retry policy can tell "the network dropped"
 * apart from "this URL will never serve audio".
 */
internal class UnusableAudioSourceException(message: String) : Exception(message)
