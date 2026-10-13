@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.shuffle

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The production module graph, with only speech — its one input from outside — replaced: the
 * shipped manifests, the scoped catalog the resolver reads, and the graph's own random source.
 */
class ShuffleGraphTest {
    @Test
    fun theCatalogAndTheResolverShareTheModuleGraph() = runTest {
        val graph = createGraphFactory<ShuffleGraph.Factory>().create(FakeSpeech())

        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        assertEquals(shuffleDecks, graph.catalog.observeDecks().first())
    }

    /** Every shipped deck asks speech for exactly its words per track, each one interval long. */
    @Test
    fun everyShippedDeckResolvesThroughSpeech() = runTest {
        val speech = FakeSpeech()
        val graph = createGraphFactory<ShuffleGraph.Factory>().create(speech)

        shuffleDecks.forEach { deck ->
            assertIs<ItemResolution.Resolved>(graph.resolver.resolve(deck.id.value))
            val words = speech.requests.last().segments.filterIsInstance<SpeechSegment.Spoken>()
            assertEquals(shuffleWordLists.single { it.deckId == deck.id.value }.wordsPerTrack, words.size)
            assertEquals(deck.wordIntervalSeconds - 0.5, words.first().minSeconds)
        }
    }
}
