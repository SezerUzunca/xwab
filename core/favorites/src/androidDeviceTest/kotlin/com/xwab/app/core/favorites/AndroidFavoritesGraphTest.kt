package com.xwab.app.core.favorites

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xwab.app.core.favorites.port.FavoriteToggleResult
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createDynamicGraphFactory
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** The Android production graph, with the Context only a device has. */
class AndroidFavoritesGraphTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * The app graph may build this graph on the main thread, so building it must touch no file; and
     * it must hand out one adapter, so one store per file.
     */
    @Test
    fun theModuleGraphSharesOneAdapterAndLeavesTheFileForItsFirstAccess() {
        // Metro warns that the Context is unused here: the file it would build is the one replaced.
        val graph = createDynamicGraphFactory<AndroidFavoritesGraph.Factory>(UnresolvableFile()).create(context)

        assertSame(graph.favorites, graph.favorites)
    }

    /** Nothing replaced: the graph's own file and scope write a favorite to disk and read it back. */
    @Test
    fun theUnmodifiedGraphWritesAFavoriteToDiskAndReadsItBack() = runBlocking {
        val favorites = createGraphFactory<AndroidFavoritesGraph.Factory>().create(context).favorites
        try {
            assertEquals(FavoriteToggleResult.Updated, favorites.setFavorite(NAMESPACE, ITEM, true))

            val snapshot = withTimeout(TIMEOUT_MS) { favorites.observe(NAMESPACE).first { ITEM in it.ids } }
            assertTrue(snapshot.isAvailable)
            val file = context.filesDir.resolve(DATA_STORE_FILE_NAME)
            assertTrue(file.length() > 0L, "nothing was written to ${file.absolutePath}")
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
