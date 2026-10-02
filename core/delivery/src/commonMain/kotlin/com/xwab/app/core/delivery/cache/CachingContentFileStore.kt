package com.xwab.app.core.delivery.cache

import co.touchlab.kermit.Logger
import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.delivery.ContentCacheLocation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
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
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class CachingContentFileStore(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val networkPort: NetworkPort,
    private val fileDispatcher: CoroutineDispatcher,
    private val legacyRoots: List<Path> = emptyList(),
) : ContentFileStore {
    /**
     * The platform names the directories. The file system and dispatcher are the real ones unless
     * a graph binds others, which is how a test graph substitutes fakes.
     */
    @Inject
    constructor(
        location: ContentCacheLocation,
        networkPort: NetworkPort,
        fileSystem: FileSystem = FileSystem.SYSTEM,
        fileDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : this(fileSystem, location.root, networkPort, fileDispatcher, location.legacyRoots)

    private val logger = Logger.withTag("CachingContentFileStore")
    private var legacyRootsPurged = false
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
