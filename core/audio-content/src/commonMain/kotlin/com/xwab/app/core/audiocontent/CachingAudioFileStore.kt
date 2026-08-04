package com.xwab.app.core.audiocontent

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * The whole of the cache's behavior, written once.
 *
 * What used to be two handwritten stores is now this class plus two pairs of adapters, because
 * only the transport and the file system calls were ever platform-shaped. Everything the two
 * copies had in common — the cache hit, the staged transfer, the promotion, the sweep of files the
 * catalog no longer refers to — reads the same on every platform and drifted precisely because it
 * was written twice.
 */
internal class CachingAudioFileStore(
    private val directory: AudioCacheDirectory,
    private val transport: AudioTransport,
) : AudioFileStore {
    override suspend fun find(cacheFileName: String): String? {
        requireSafeName(cacheFileName)

        val size = directory.sizeOf(cacheFileName) ?: return null
        if (size > 0L) return directory.localUri(cacheFileName)
        // An empty file is a leftover from a failed write, not a cache hit.
        directory.delete(cacheFileName)
        return null
    }

    override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
        require(remoteHttpsUrl.startsWith("https://")) { "Only HTTPS audio downloads are allowed." }
        requireSafeName(cacheFileName)
        directory.prepare()

        if ((directory.sizeOf(cacheFileName) ?: 0L) > 0L) return
        directory.delete(cacheFileName)

        // Staged under a name that fails the cache pattern, so a concurrent sweep leaves it alone
        // and a reader never mistakes an unfinished transfer for a playable file.
        val partial = partialCacheFileName(cacheFileName)
        directory.delete(partial)
        try {
            transport.fetchInto(remoteHttpsUrl, partial)

            val downloadedBytes = directory.sizeOf(partial)
            check(downloadedBytes != null && downloadedBytes > 0L) { "Downloaded audio is empty." }
            requireWithinSizeLimit(downloadedBytes)
            directory.promote(partial, cacheFileName)
            removeUnreferencedFiles()
        } finally {
            // Outside cancellation on purpose: an adapter crosses a dispatcher on every call, so a
            // cancelled download would throw on the way into the delete and leave its staged file
            // on disk — where nothing else would ever clear it, since a sweep skips partial names.
            withContext(NonCancellable) { directory.delete(partial) }
        }
    }

    /**
     * Runs after a completed download rather than at startup: it is the one moment the cache is
     * known to have just changed, and the directory listing it costs is already paid for by the
     * transfer that preceded it.
     */
    private suspend fun removeUnreferencedFiles() {
        unreferencedCacheFileNames(directory.list(), catalogCacheFileNames).forEach { name ->
            directory.delete(name)
        }
    }

    /** The last gate before a name reaches a file system, so a manifest typo cannot traverse. */
    private fun requireSafeName(cacheFileName: String) {
        require(CACHE_FILE_NAME.matches(cacheFileName)) { "Unsafe audio cache file name." }
    }
}

/**
 * The file system half of the cache, reduced to what [CachingAudioFileStore] actually asks of it.
 *
 * Everything here is a primitive with no policy in it: no decision about what is worth keeping, no
 * naming rule, no size limit. That is deliberate — those rules are the same on every platform, so
 * they live in the store above and are tested there, and an implementation of this port only has to
 * translate six operations into `File` or `NSFileManager`.
 *
 * Names arriving here have already been checked by the store. They are plain file names inside the
 * cache root, never paths, and a staged transfer is named so that it fails [CACHE_FILE_NAME].
 */
internal interface AudioCacheDirectory {
    /** Creates the cache root if it is missing, and applies whatever the platform asks of it. */
    suspend fun prepare()

    /** Size of the regular file [fileName], or `null` when nothing usable is there. */
    suspend fun sizeOf(fileName: String): Long?

    /** Removes [fileName] if it exists. A missing file is not an error. */
    suspend fun delete(fileName: String)

    /** The URI a player can open [fileName] with. Naming only, so it asks the disk nothing. */
    fun localUri(fileName: String): String

    /** Every name directly inside the cache root, including staged transfers. */
    suspend fun list(): List<String>

    /** Moves a completed transfer onto its final name, replacing nothing — the store clears it. */
    suspend fun promote(partialFileName: String, cacheFileName: String)
}

/**
 * The network half: the one thing `HttpsURLConnection` and `NSURLSession` cannot be talked about in
 * common terms.
 *
 * An implementation streams to disk rather than into memory — a track is megabytes and the phone is
 * asleep — and applies the shared answer rules as the response arrives, so that a source which will
 * never serve audio is raised as [UnusableAudioSourceException] and costs no further attempts.
 */
internal interface AudioTransport {
    /**
     * Fetches [remoteHttpsUrl] into [partialFileName] inside the cache root.
     *
     * Returning normally means the bytes are on disk under that name; the store decides whether
     * they are worth keeping. A partial file left behind by a failure is the store's to clean up.
     */
    suspend fun fetchInto(remoteHttpsUrl: String, partialFileName: String)
}
