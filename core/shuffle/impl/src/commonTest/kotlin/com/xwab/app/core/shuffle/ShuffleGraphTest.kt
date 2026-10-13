@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * The bindings every platform's graph includes, over the real file system: the production
 * manifests, the renderer on its own dispatcher and the resolver that reads the catalog the graph
 * scopes. Only the two platform inputs are stand-ins — a voice no host has, and a cache directory
 * under the system's temporary one. The platform graphs themselves are built on a device and on the
 * simulator.
 */
class ShuffleGraphTest {
    @Test
    fun theCatalogAndTheResolverShareTheModuleGraph() {
        val graph = graphIn(FileSystem.SYSTEM_TEMPORARY_DIRECTORY)

        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        assertEquals(shuffleDecks, runBlocking { graph.catalog.observeDecks().first() })
    }

    /** Every shipped deck renders through the graph into a file of exactly its length. */
    @Test
    fun everyShippedDeckRendersToDisk() = runBlocking {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "shuffle-graph-${Random.nextLong().toULong()}"
        try {
            val graph = graphIn(root)
            shuffleDecks.forEach { deck ->
                val resolved = assertIs<ItemResolution.Resolved>(graph.resolver.resolve(deck.id.value))
                val track = Wav.decode(FileSystem.SYSTEM.read(resolved.uri.toPath()) { readByteArray() })
                val words = shuffleWordLists.single { it.deckId == deck.id.value }.wordsPerTrack
                assertEquals(words * deck.wordIntervalSeconds * VOICE_RATE, track.samples.size)
            }
        } finally {
            FileSystem.SYSTEM.deleteRecursively(root)
        }
    }

    private fun graphIn(root: Path): ShuffleGraph = createGraphFactory<TestShuffleGraph.Factory>()
        .create(voice = FakeVoice(FileSystem.SYSTEM, sampleRate = VOICE_RATE), location = { root })

    private companion object {
        const val VOICE_RATE = 1_000
    }
}

/** The common bindings with the platform's two inputs handed in, as each platform graph binds them. */
@DependencyGraph(ShuffleScope::class, bindingContainers = [ShuffleBindings::class])
internal interface TestShuffleGraph : ShuffleGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides voice: WordVoice, @Provides location: ShuffleCacheLocation): TestShuffleGraph
    }
}
