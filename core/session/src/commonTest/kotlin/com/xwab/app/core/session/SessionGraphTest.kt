@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.runBlocking

class SessionGraphTest {
    /** The production module graph hands an item to the engine through the resolvers it is given. */
    @Test
    fun theModuleGraphPlaysThroughTheResolversItIsGiven() = runBlocking {
        val engine = EmptyContentPlaybackEngine()
        val graph = createGraphFactory<SessionGraph.Factory>().create(
            enginePort = engine,
            resolversByKind = mapOf(TEST_KIND to TestResolver),
        )

        assertSame(graph.playback, graph.playback)
        graph.playback.play(PlaybackItemId(TEST_KIND, "rain"))

        assertEquals(1, engine.submittedCommands)
    }

    private object TestResolver : PlaybackItemResolver {
        override suspend fun resolve(value: String): ItemResolution = ItemResolution.Resolved(
            uri = "https://example.test/$value.mp3",
            title = value,
            displayName = value,
            artist = null,
            policy = PlaybackPolicy(looping = false),
        )
    }
}

private const val TEST_KIND = "test"
