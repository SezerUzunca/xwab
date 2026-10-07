@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.CacheKey
import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SoundPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class SoundGraphTest {
    /** The production graph wires playback and offline availability to the given delivery. */
    @Test
    fun theModuleGraphResolvesAShippedTrackThroughTheGivenDelivery() = runBlocking {
        val delivery = RecordingDelivery()
        val graph = createGraphFactory<SoundGraph.Factory>().create(delivery)
        val track = graph.catalog.observeAllTracks().first().first()

        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        val resolved = assertIs<ItemResolution.Resolved>(graph.resolver.resolve(track.id.value))
        assertEquals("https://example.test/delivered.mp3", resolved.uri)
        assertEquals(track.name, resolved.title)

        assertTrue(graph.catalog.observeOfflineReady(track.id).first())
        assertEquals(delivery.requests.single().key, delivery.observed.single())
    }

    /**
     * The application graph, as the session sees it: the sound kind's provider hands out one
     * resolver bridge, built on the first call and reused after, over the one module graph the
     * catalog bridge also uses.
     */
    @Test
    fun theApplicationGraphReusesOneResolverBridgeForTheSoundKind(): Unit = runBlocking {
        val graph = createGraph<SoundContributionGraph>()
        val provider = graph.resolvers.getValue(SOUND_PLAYBACK_KIND)

        assertSame(provider(), provider())
        val track = graph.catalog.observeAllTracks().first().first()
        assertIs<ItemResolution.Resolved>(provider().resolve(track.id.value))
    }

    private class RecordingDelivery : DeliveryPort {
        val requests = mutableListOf<DeliveryRequest>()
        val observed = mutableListOf<CacheKey>()

        override suspend fun resolve(request: DeliveryRequest): String {
            requests += request
            return "https://example.test/delivered.mp3"
        }

        override fun observeCached(key: CacheKey): Flow<Boolean> {
            observed += key
            return flowOf(true)
        }
    }
}

@DependencyGraph(AppScope::class)
internal interface SoundContributionGraph {
    val resolvers: Map<String, () -> PlaybackItemResolver>
    val catalog: SoundPort
}

// Higher priority replaces the platform delivery without referencing its module-private bridge.
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, priority = 1)
internal class SoundTestDelivery : DeliveryPort {
    override suspend fun resolve(request: DeliveryRequest): String = request.httpsUrl

    override fun observeCached(key: CacheKey): Flow<Boolean> = flowOf(false)
}
