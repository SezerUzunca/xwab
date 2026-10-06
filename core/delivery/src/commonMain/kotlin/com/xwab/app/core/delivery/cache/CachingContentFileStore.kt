package com.xwab.app.core.delivery.cache

import co.touchlab.kermit.Logger
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.delivery.ContentCacheLocation
import com.xwab.app.core.delivery.DeliveryScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
// Required on Kotlin/Native for FileHandle; the JVM stdlib equivalent does not cover it.
import okio.use

// Scoped, not merely injected: the prefetcher writes into the same store the delivery adapter
// observes, and only that shared instance tells an open screen a download has finished.
@SingleIn(DeliveryScope::class)
internal class CachingContentFileStore(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val networkPort: NetworkPort,
    private val fileDispatcher: CoroutineDispatcher,
) : ContentFileStore {
    /**
     * The platform names the directory. The file system and dispatcher come from the module graph,
     * which a test replaces with fakes.
     */
    @Inject
    constructor(
        location: ContentCacheLocation,
        networkPort: NetworkPort,
        fileSystem: FileSystem,
        fileDispatcher: CoroutineDispatcher,
    ) : this(fileSystem, location.root, networkPort, fileDispatcher)

    private val logger = Logger.withTag("CachingContentFileStore")
    private val cacheRevision = MutableStateFlow(0L)

    override fun observeCached(key: CacheKey): Flow<Boolean> = cacheRevision.map {
        try {
            withContext(fileDispatcher) {
                val metadata = fileSystem.metadataOrNull(pathOf(key))
                metadata?.isRegularFile == true && (metadata.size ?: 0L) > 0L
            }
        } catch (error: IOException) {
            logger.w(error) { "Could not inspect cached content for $key." }
            false
        }
    }.distinctUntilChanged()

    override suspend fun find(key: CacheKey): String? = withContext(fileDispatcher) {
        val file = pathOf(key)
        val metadata = fileSystem.metadataOrNull(file)
        if (metadata?.isRegularFile != true) return@withContext null
        if ((metadata.size ?: 0L) > 0L) return@withContext file.toString()
        fileSystem.delete(file, mustExist = false)
        cacheRevision.update { it + 1 }
        null
    }

    override suspend fun download(request: DeliveryRequest) {
        if (find(request.key) != null) {
            cacheRevision.update { it + 1 }
            return
        }
        val directory = root / request.key.namespace
        val partial = directory / partialCacheFileName(request.key.fileName)
        withContext(fileDispatcher) {
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
                try {
                    fileSystem.delete(partial, mustExist = false)
                } finally {
                    // Publish only after promotion/cleanup. A partial transfer is never ready.
                    // Also recheck files removed by a partially completed inventory sweep.
                    cacheRevision.update { it + 1 }
                }
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
