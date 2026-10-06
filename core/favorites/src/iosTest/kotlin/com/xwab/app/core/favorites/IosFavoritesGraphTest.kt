package com.xwab.app.core.favorites

import com.xwab.app.core.favorites.port.FavoriteToggleResult
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createDynamicGraph
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSFileManager

/** The iOS production graph, with its favorites file built by Metro from the system's file manager. */
class IosFavoritesGraphTest {
    /**
     * The app graph may build this graph on the main thread, so building it must touch no file; and
     * it must hand out one adapter, so one store per file.
     */
    @Test
    fun theModuleGraphSharesOneAdapterAndLeavesTheFileForItsFirstAccess() {
        val graph = createDynamicGraph<IosFavoritesGraph>(UnresolvableFile())

        assertSame(graph.favorites, graph.favorites)
    }

    @Test
    fun theFileLivesInTheDocumentsDirectory() {
        val path = IosFavoritesFile(NSFileManager.defaultManager).path().toString()

        assertTrue(path.endsWith("Documents/$DATA_STORE_FILE_NAME"), path)
    }

    /**
     * Nothing replaced: the graph's own file and scope write a favorite to disk and read it back.
     * The simulator keeps its documents between runs, so the test removes what it added.
     */
    @Test
    fun theUnmodifiedGraphWritesAFavoriteToDiskAndReadsItBack() = runBlocking {
        val favorites = createGraph<IosFavoritesGraph>().favorites
        try {
            assertEquals(FavoriteToggleResult.Updated, favorites.setFavorite(NAMESPACE, ITEM, true))

            val snapshot = withTimeout(TIMEOUT_MS) { favorites.observe(NAMESPACE).first { ITEM in it.ids } }
            assertTrue(snapshot.isAvailable)
            val path = IosFavoritesFile(NSFileManager.defaultManager).path().toString()
            assertTrue(NSFileManager.defaultManager.fileExistsAtPath(path), "nothing was written to $path")
        } finally {
            favorites.setFavorite(NAMESPACE, ITEM, false)
        }
    }

    /** Replaces the platform's favorites file with one that fails the test if anything resolves it. */
    @BindingContainer
    private class UnresolvableFile {
        @Provides
        fun file(): FavoritesFile =
            FavoritesFile { error("The DataStore file was resolved while the graph was built.") }
    }

    private companion object {
        const val NAMESPACE = "graph-test"
        const val ITEM = "persisted"
        const val TIMEOUT_MS = 10_000L
    }
}
