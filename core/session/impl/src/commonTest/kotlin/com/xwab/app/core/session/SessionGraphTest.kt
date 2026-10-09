@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.port.SleepTimerState
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackResolverApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SessionGraphTest {
    /** Building the session does not construct the content graphs registered in its provider map. */
    @Test
    fun theModuleGraphCreatesOnlyTheResolverNeededForPlayback() = runBlocking {
        val engine = SessionTestPlaybackEngine()
        var resolverCreations = 0
        val graph = createGraphFactory<SessionGraph.Factory>().create(
            enginePort = engine,
            resolvers = ContentResolvers(
                mapOf(
                    TEST_KIND to {
                        resolverCreations++
                        SessionGraphTestResolver
                    },
                    "unused" to { error("An unrelated resolver must not be constructed.") },
                ),
            ),
        )

        assertSame(graph.playback, graph.playback)
        assertEquals(0, resolverCreations)

        graph.playback.play(PlaybackItemId("absent", "rain"))
        assertEquals(0, resolverCreations)
        assertEquals(0, engine.submittedCommands)

        graph.playback.play(PlaybackItemId(TEST_KIND, "rain"))

        assertEquals(1, resolverCreations)
        assertEquals(1, engine.submittedCommands)
    }

    @Test
    fun resumingAnAlreadyHeldSourceDoesNotConstructItsResolver() = runBlocking {
        val engine = SessionTestPlaybackEngine().apply {
            state.value = AudioPlayerState(
                source = AudioSource("$TEST_KIND:rain", "https://example.test/rain.mp3"),
                phase = PlaybackPhase.Ready,
            )
        }
        val graph = createGraphFactory<SessionGraph.Factory>().create(
            enginePort = engine,
            resolvers = ContentResolvers(mapOf(TEST_KIND to { error("Resuming needs no source lookup.") })),
        )

        graph.playback.play(PlaybackItemId(TEST_KIND, "rain"))

        assertEquals(1, engine.submittedCommands)
    }

    /** Metro lifts real map contributions into providers before the bridge passes them to the module. */
    @Test
    fun theApplicationGraphSuppliesTheContributedResolverAsAProvider() = runBlocking {
        val graph = createGraph<SessionContributionGraph>()
        val engine = assertIs<SessionTestPlaybackEngine>(graph.engine)

        assertSame(graph.session, graph.session)
        graph.session.play(PlaybackItemId(TEST_KIND, "rain"))

        assertEquals(1, engine.submittedCommands)
    }

    @Test
    fun aSessionCanBeInjectedWithoutAnyContentContributions() = runBlocking {
        val graph = createGraph<EmptyContentSessionGraph>()
        val engine = assertIs<SessionTestPlaybackEngine>(graph.engine)
        val removedItem = PlaybackItemId("removed-content", "item")

        graph.session.play(removedItem)

        assertEquals(PlaybackFailure.ItemNotFound(removedItem), graph.session.playback.first().failure)
        assertEquals(0, engine.submittedCommands)
    }
}

@DependencyGraph(AppScope::class)
internal interface SessionContributionGraph {
    val session: PlaybackPort
    val engine: PlaybackEnginePort
}

// Exclude the test resolver to exercise the optional empty map independently of map contributions.
@DependencyGraph(AppScope::class, excludes = [SessionGraphTestResolver::class])
internal interface EmptyContentSessionGraph {
    val session: PlaybackPort
    val engine: PlaybackEnginePort
}

// Higher priority replaces the platform engine without referencing its module-private class.
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, priority = 1)
internal class SessionTestPlaybackEngine : PlaybackEnginePort {
    override val state = MutableStateFlow(AudioPlayerState())
    override val sleepTimerState = MutableStateFlow(SleepTimerState())
    var submittedCommands = 0
        private set

    override fun submit(command: PlaybackCommand) {
        submittedCommands++
    }

    override fun release() = Unit
}

@ContributesIntoMap(AppScope::class)
@StringKey(TEST_KIND)
internal object SessionGraphTestResolver : PlaybackItemResolver {
    override suspend fun resolve(value: String): ItemResolution = ItemResolution.Resolved(
        uri = "https://example.test/$value.mp3",
        title = value,
        displayName = value,
        artist = null,
        policy = PlaybackPolicy(looping = false),
    )
}

private const val TEST_KIND = "test"
