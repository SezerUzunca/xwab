@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.sound

import com.xwab.app.core.delivery.port.DeliveryPort
import com.xwab.app.core.delivery.port.DeliveryRequest
import com.xwab.app.core.delivery.port.DeliveryResult
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SoundGraphTest {
    /** The production module graph: one catalog, which the resolver reads, over the given delivery. */
    @Test
    fun theModuleGraphResolvesAShippedTrackThroughTheGivenDelivery() = runBlocking {
        val graph = createGraphFactory<SoundGraph.Factory>().create(ResolvingDelivery)
        val track = graph.catalog.observeAllTracks().first().first()

        assertSame(graph.catalog, graph.catalog)
        val resolved = assertIs<ItemResolution.Resolved>(graph.resolver.resolve(track.id.value))
        assertEquals("https://example.test/delivered.mp3", resolved.uri)
        assertEquals(track.name, resolved.title)
    }

    private object ResolvingDelivery : DeliveryPort {
        override suspend fun resolve(request: DeliveryRequest): DeliveryResult =
            DeliveryResult.Resolved("https://example.test/delivered.mp3")
    }
}
