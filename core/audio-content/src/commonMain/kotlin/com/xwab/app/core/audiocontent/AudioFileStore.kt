package com.xwab.app.core.audiocontent

/**
 * Platform storage and transport boundary for remotely hosted audio.
 *
 * Implementations must write to a temporary file and only expose a final local URI after the
 * download has completed successfully. A completed download also sweeps every file the catalog no
 * longer refers to, so the cache holds one version per track and nothing for a track that is gone.
 */
interface AudioFileStore {
    suspend fun find(cacheFileName: String): String?

    /**
     * Fetches [cacheFileName] from [remoteHttpsUrl], or returns without transferring anything when
     * a usable copy is already on disk. Callers do not check first: only the store can weigh the
     * cache against the write it is about to make, so the check belongs on this side of the port.
     */
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
