package com.xwab.app.core.audiocontent

import co.touchlab.kermit.Logger
import com.xwab.app.core.domain.port.AudioContentResolver
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.ResolvedAudioContent
import com.xwab.app.core.model.Category
import com.xwab.app.core.model.Music
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class HybridAudioContentRepository(
    private val fileStore: AudioFileStore,
    private val resourceUri: (String) -> String = ::bundledResourceUri,
    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : MusicCatalogRepository, AudioContentResolver {
    private val logger = Logger.withTag("AudioContent")
    private val entriesById = catalogEntries.associateBy { it.music.id }
    private val categories = flowOf(catalogCategories)
    private val tracks = flowOf(catalogEntries.map(CatalogEntry::music))
    private val queuedDownloads = mutableSetOf<String>()
    private val queueMutex = Mutex()

    override fun observeCategories(): Flow<List<Category>> = categories

    override fun observeAllMusic(): Flow<List<Music>> = tracks

    override fun observeCategory(categoryId: String): Flow<Category?> =
        categories.map { values -> values.find { it.id == categoryId } }

    override fun observeMusicForCategory(categoryId: String): Flow<List<Music>> =
        tracks.map { values -> values.filter { it.categoryId == categoryId } }

    override fun observeMusic(musicId: String): Flow<Music?> =
        tracks.map { values -> values.find { it.id == musicId } }

    override suspend fun resolve(musicId: String): ResolvedAudioContent? {
        val entry = entriesById[musicId] ?: return null
        return when (val asset = entry.asset) {
            is AudioAssetSpec.Bundled ->
                ResolvedAudioContent(resourceUri(asset.resourcePath), isLocal = true)

            is AudioAssetSpec.Remote -> {
                val fileName = asset.cacheFileName(musicId)
                val localUri = fileStore.find(fileName)
                if (localUri != null) {
                    ResolvedAudioContent(localUri, isLocal = true)
                } else {
                    queueDownload(musicId, fileName, asset.httpsUrl)
                    ResolvedAudioContent(asset.httpsUrl, isLocal = false)
                }
            }
        }
    }

    fun close() {
        backgroundScope.cancel()
    }

    private suspend fun queueDownload(musicId: String, fileName: String, remoteUrl: String) {
        val shouldLaunch = queueMutex.withLock { queuedDownloads.add(musicId) }
        if (!shouldLaunch) return

        backgroundScope.launch {
            try {
                downloadWithRetry(fileName, remoteUrl)
            } finally {
                queueMutex.withLock { queuedDownloads.remove(musicId) }
            }
        }
    }

    private suspend fun downloadWithRetry(fileName: String, remoteUrl: String) {
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                if (fileStore.find(fileName) == null) fileStore.download(fileName, remoteUrl)
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val isLastAttempt = attempt == DOWNLOAD_ATTEMPTS - 1
                if (isLastAttempt) {
                    logger.w(error) { "Could not cache $fileName; playback will keep using HTTPS." }
                } else {
                    delay(RETRY_BASE_DELAY_MS * (1L shl attempt))
                }
            }
        }
    }

    private companion object {
        const val DOWNLOAD_ATTEMPTS = 3
        const val RETRY_BASE_DELAY_MS = 500L
    }
}
