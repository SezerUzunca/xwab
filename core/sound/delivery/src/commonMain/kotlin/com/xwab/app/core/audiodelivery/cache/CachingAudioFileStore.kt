package com.xwab.app.core.audiodelivery.cache

import com.xwab.app.core.catalogmanifest.AudioSourceCatalog
import com.xwab.app.core.catalogmanifest.CACHE_FILE_NAME
import com.xwab.app.core.network.NetworkClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/**
 * The complete on-demand audio cache, shared by Android and iOS.
 *
 * `Dispatchers.IO` is a member on JVM but only an `expect val Dispatchers.IO` extension on
 * Kotlin/Native — a member always wins over an extension of the same name, so without the explicit
 * `import kotlinx.coroutines.IO` above, Native falls through to the library's internal `IO` and
 * fails to compile. An IDE's "optimize imports" does not know that and will remove it as
 * apparently unused, since the JVM source set never needed it — do not let it.
 */
internal class CachingAudioFileStore(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val network: NetworkClient,
    private val sourceCatalog: AudioSourceCatalog,
    private val fileDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AudioFileStore {
    override suspend fun find(cacheFileName: String): String? {
        requireSafeName(cacheFileName)
        return withContext(fileDispatcher) {
            val file = pathOf(cacheFileName)
            val metadata = fileSystem.metadataOrNull(file)
            if (metadata?.isRegularFile != true) return@withContext null
            if ((metadata.size ?: 0L) > 0L) return@withContext file.toString()

            // A zero-length file is an interrupted write, not playable content.
            fileSystem.delete(file, mustExist = false)
            null
        }
    }

    override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) {
        requireSafeName(cacheFileName)
        if (find(cacheFileName) != null) return

        val partial = pathOf(partialCacheFileName(cacheFileName))
        withContext(fileDispatcher) {
            fileSystem.createDirectories(root)
            fileSystem.delete(partial, mustExist = false)
        }

        try {
            withContext(fileDispatcher) {
                writeDownload(partial, remoteHttpsUrl)

                val downloadedBytes = fileSystem.metadataOrNull(partial)?.size
                check(downloadedBytes != null && downloadedBytes > 0L) {
                    "Downloaded audio is empty."
                }
                requireWithinSizeLimit(downloadedBytes)
                fileSystem.atomicMove(partial, pathOf(cacheFileName))
                removeUnreferencedFiles()
            }
        } finally {
            // A canceled transfer must not strand a `.part` file that the normal sweep ignores.
            withContext(NonCancellable + fileDispatcher) {
                fileSystem.delete(partial, mustExist = false)
            }
        }
    }

    /**
     * A [FileSystem.openReadWrite] handle is used instead of a plain sink so `FileHandle.flush()`
     * reaches the platform file handle before the staged file is promoted.
     *
     * The buffered sink is closed before the handle is flushed, so application buffers are gone
     * before the platform is asked to push anything.
     *
     * Okio only promises that `flush()` "pushes all buffered bytes to their final destination", and
     * the implementations differ: the JVM calls `fd.sync()`, the Unix/Apple one calls `fflush`. So
     * the bytes are on the device before the move on Android, and in the kernel's hands on iOS.
     * The README explains what that leaves open.
     *
     * The explicit `import okio.use` above is load-bearing. `okio.Closeable` is `actual typealias
     * Closeable = java.io.Closeable` on the JVM, so `FileHandle.use { }` resolves through the JVM
     * stdlib's own `kotlin.io.use` there without ever needing Okio's version. On Kotlin/Native,
     * `okio.Closeable` is Okio's own bespoke interface — it extends neither `java.io.Closeable` nor
     * `kotlin.AutoCloseable` — so the common `AutoCloseable.use` the compiler finds instead does not
     * apply, and only `okio.use` resolves. Verified against Okio 3.17.0's own sources
     * (`okio/-JvmPlatform.kt` vs. `okio/NonJvmPlatform.kt`).
     */
    private suspend fun writeDownload(partial: Path, remoteHttpsUrl: String) {
        fileSystem.openReadWrite(partial, mustCreate = true, mustExist = false).use { handle ->
            handle.sink().buffer().use { sink ->
                network.downloadAudio(remoteHttpsUrl) { bytes, count ->
                    sink.write(bytes, 0, count)
                }
            }
            handle.flush()
        }
    }

    private fun removeUnreferencedFiles() {
        val cachedNames = fileSystem.list(root).map(Path::name)
        unreferencedCacheFileNames(cachedNames, sourceCatalog.cacheFileNames).forEach { name ->
            fileSystem.delete(pathOf(name), mustExist = false)
        }
    }

    private fun pathOf(fileName: String): Path = root / fileName

    private fun requireSafeName(cacheFileName: String) {
        require(CACHE_FILE_NAME.matches(cacheFileName)) { "Unsafe audio cache file name." }
    }
}
