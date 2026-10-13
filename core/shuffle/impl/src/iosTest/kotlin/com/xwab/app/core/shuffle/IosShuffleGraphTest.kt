package com.xwab.app.core.shuffle

import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager

/**
 * The iOS production graph. It is built and read here; it does not speak, because whether the
 * simulator's synthesizer writes audio is not something CI can rely on — the same reason no iOS
 * test here plays media.
 */
class IosShuffleGraphTest {
    @Test
    fun theModuleGraphSharesItsCatalogAndListsTheShippedDecks() = runBlocking {
        val graph = createGraph<IosShuffleGraph>()

        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        assertEquals(shuffleDecks, graph.catalog.observeDecks().first())
    }

    @Test
    fun wordsAndTracksLiveInTheCachesDirectory() {
        val root = IosShuffleCacheLocation(NSFileManager.defaultManager).root().toString()

        assertTrue(root.endsWith("Caches/shuffle"), root)
    }
}
