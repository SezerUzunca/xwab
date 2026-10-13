package com.xwab.app.core.speech

import com.xwab.app.core.speech.port.NoiseBed
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult
import com.xwab.app.core.speech.port.SpeechSegment
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * The bindings every platform's graph includes, over the real file system: the renderer on its own
 * dispatcher with the module's random source. Only the two platform inputs are stand-ins — a voice
 * no host has, and a cache directory under the system's temporary one. The platform graphs
 * themselves are built on a device and on the simulator.
 */
class SpeechGraphTest {
    @Test
    fun theGraphHandsOutOneRenderer() {
        val graph = graphIn(FileSystem.SYSTEM_TEMPORARY_DIRECTORY)

        assertSame(graph.speech, graph.speech)
    }

    /** A track with a bed renders to disk at exactly its laid-out length. */
    @Test
    fun aTrackRendersToTheRealFileSystem() = runBlocking {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "speech-graph-${Random.nextLong().toULong()}"
        try {
            val request = SpeechRequest(
                folder = "guide",
                name = "test",
                languageTag = "en-US",
                segments = listOf(SpeechSegment.Pause(1.0), SpeechSegment.Spoken("rest", minSeconds = 2.0)),
                bed = NoiseBed(level = 0.03, fadeSeconds = 0.5),
                reuse = true,
            )

            val result = assertIs<SpeechResult.Rendered>(graphIn(root).speech.render(request))

            val track = Wav.decode(FileSystem.SYSTEM.read(result.path.toPath()) { readByteArray() })
            assertEquals((1.0 + 2.0) * VOICE_RATE, track.samples.size.toDouble())
        } finally {
            FileSystem.SYSTEM.deleteRecursively(root)
        }
    }

    private fun graphIn(root: Path): SpeechGraph = createGraphFactory<TestSpeechGraph.Factory>()
        .create(voice = FakeVoice(FileSystem.SYSTEM, sampleRate = VOICE_RATE), location = { root })

    private companion object {
        const val VOICE_RATE = 1_000
    }
}

/** The common bindings with the platform's two inputs handed in, as each platform graph binds them. */
@DependencyGraph(SpeechScope::class, bindingContainers = [SpeechBindings::class])
internal interface TestSpeechGraph : SpeechGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides voice: Voice, @Provides location: SpeechCacheLocation): TestSpeechGraph
    }
}
