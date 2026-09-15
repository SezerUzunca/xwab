package com.xwab.app.core.session

import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.sources.port.SOUND_NAMESPACE
import com.xwab.app.core.sources.port.STORY_NAMESPACE
import com.xwab.app.core.sources.port.SourcePort
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Replaces the compile-time coupling lost when metadata and physical addresses were separated.
 *
 * It belongs to this module because pairing published content with the address it plays from is
 * what the session does: [SoundPlaybackResolver] and [StoryPlaybackResolver] make exactly this
 * lookup at runtime, and the three ports the check needs are already this module's dependencies.
 */
class ContentSourceConsistencyTest {
    @Test
    fun everyPublishedContentItemHasAPhysicalSource() = runBlocking {
        val graph = createGraph<ContentSourceConsistencyGraph>()

        graph.soundPort.observeAllTracks().first().forEach { track ->
            assertNotNull(
                graph.sourcePort.sourceFor(SOUND_NAMESPACE, track.id.value),
                "${track.id} has no physical source",
            )
        }
        graph.storyPort.observeStories().first().forEach { story ->
            assertNotNull(
                graph.sourcePort.sourceFor(STORY_NAMESPACE, story.id.value),
                "${story.id} has no physical source",
            )
        }
    }
}

/**
 * The three catalog ports, and nothing else.
 *
 * The scope's other contributions — the delivery, playback and platform adapters this module also
 * depends on — are on the classpath but unreachable from these three accessors, and Metro drops
 * bindings no accessor reaches. That is what lets a test graph ask for a slice of `AppScope`
 * without providing the platform values the rest of it is built from.
 */
@DependencyGraph(AppScope::class)
internal interface ContentSourceConsistencyGraph {
    val soundPort: SoundPort
    val storyPort: StoryPort
    val sourcePort: SourcePort
}
