package com.xwab.app.core.delivery.cache

import co.touchlab.kermit.Logger
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
// Required on Kotlin/Native for FileHandle; the JVM stdlib equivalent does not cover it.
import okio.use

internal class CachingContentFileStore(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val networkPort: NetworkPort,
    private val fileDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val legacyRoots: List<Path> = emptyList(),
) : ContentFileStore {
    private val logger = Logger.withTag("CachingContentFileStore")
    private var legacyRootsPurged = false

    override suspend fun find(key: CacheKey): String? = withContext(fileDispatcher) {
        val file = pathOf(key)
        val metadata = fileSystem.metadataOrNull(file)
        if (metadata?.isRegularFile != true) return@withContext null
        if ((metadata.size ?: 0L) > 0L) return@withContext file.toString()
        fileSystem.delete(file, mustExist = false)
        null
    }

    override suspend fun download(request: DeliveryRequest) {
        if (find(request.key) != null) return
        val directory = root / request.key.namespace
        val partial = directory / partialCacheFileName(request.key.fileName)
        withContext(fileDispatcher) {
            purgeLegacyRootsOnce()
            fileSystem.createDirectories(directory)
            fileSystem.delete(partial, mustExist = false)
        }
        try {
            withContext(fileDispatcher) {
                writeDownload(partial, request)
                val bytes = fileSystem.metadataOrNull(partial)?.size
                check(bytes != null && bytes > 0L) { "Downloaded content is empty." }
                requireWithinSizeLimit(bytes, request.maxBytes)
                fileSystem.atomicMove(partial, pathOf(request.key))
                request.retainedFileNames?.let { removeUnreferencedFiles(directory, it) }
            }
        } finally {
            withContext(NonCancellable + fileDispatcher) {
                fileSystem.delete(partial, mustExist = false)
            }
        }
    }

    private suspend fun writeDownload(partial: Path, request: DeliveryRequest) {
        fileSystem.openReadWrite(partial, mustCreate = true, mustExist = false).use { handle ->
            handle.sink().buffer().use { sink ->
                networkPort.downloadContent(request) { bytes, count -> sink.write(bytes, 0, count) }
            }
            handle.flush()
        }
    }

    /**
     * Drops caches written before delivery was namespaced.
     *
     * Those files sit beside the namespace directories rather than inside one, and the sweep only
     * ever lists a single namespace — so nothing here would reach them again, and they would stay
     * until the platform reclaimed the cache on its own. One directory walk on the first download
     * after an upgrade settles it. A cache that refuses to be deleted is not worth failing the
     * download that happened to find it.
     */
    private fun purgeLegacyRootsOnce() {
        if (legacyRootsPurged) return
        legacyRootsPurged = true
        legacyRoots.forEach { legacyRoot ->
            try {
                fileSystem.deleteRecursively(legacyRoot, mustExist = false)
            } catch (error: IOException) {
                logger.w(error) { "Could not remove the pre-namespace cache at $legacyRoot." }
            }
        }
    }

    private fun removeUnreferencedFiles(directory: Path, keep: Set<String>) {
        val cachedNames = fileSystem.list(directory)
            .filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }
            .map(Path::name)
        unreferencedCacheFileNames(cachedNames, keep).forEach { name ->
            fileSystem.delete(directory / name, mustExist = false)
        }
    }

    private fun pathOf(key: CacheKey): Path = root / key.namespace / key.fileName
}

internal val CACHE_FILE_NAME = Regex("[a-z0-9][a-z0-9_-]*(?:\\.[a-z0-9]+)?")
