@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.story

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.story.port.StoryPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StorySourceManifestTest {
    @Test
    fun metadataAndPhysicalSourcesHaveExactlyTheSameIds() {
        assertEquals(
            storyManifest.map { it.id.value }.toSet(),
            storySourceManifest.map { it.itemId }.toSet(),
            "Each story owns both its metadata and its source; neither may be orphaned.",
        )
    }

    @Test
    fun theInstalledResolverCanPlayEveryPublishedStory() = runBlocking {
        val graph = createGraph<StoryResolutionGraph>()
        assertEquals(setOf(STORY_PLAYBACK_KIND), graph.resolvers.keys)
        val publishedSources = storySourceManifest.associateBy(StorySource::itemId)
        val resolver = graph.resolvers.getValue(STORY_PLAYBACK_KIND)

        graph.catalog.observeStories().first().forEach { story ->
            val resolved = assertIs<ItemResolution.Resolved>(resolver.resolve(story.id.value))
            assertEquals(publishedSources.getValue(story.id.value).httpsUrl, resolved.uri)
            assertEquals(story.title, resolved.title)
            assertEquals(story.title, resolved.displayName)
            assertEquals(story.narrator, resolved.artist)
            assertEquals(false, resolved.policy.defaultLooping)
        }
    }

    @Test
    fun shippedSourceRowsHaveUniqueIdsAndMp3Addresses() {
        val urls = storySourceManifest.map(StorySource::httpsUrl)

        assertEquals(storySourceManifest.size, storySourceManifest.map { it.itemId }.toSet().size)
        assertEquals(urls.size, urls.toSet().size, "duplicate source URLs")
        assertTrue(urls.all { it.endsWith(".mp3") })
    }

    @Test
    fun physicalSourcesRequireNonblankIdsAndHttpsAddresses() {
        listOf("", " ").forEach { id ->
            assertFailsWith<IllegalArgumentException> { StorySource(id, "https://example.test/story.mp3") }
        }
        listOf("", "http://example.test/story.mp3", "file:///story.mp3", "https://", "https://host/a b.mp3")
            .forEach { address ->
                assertFailsWith<IllegalArgumentException>(address) { StorySource("story", address) }
            }
    }
}

@DependencyGraph(AppScope::class)
internal interface StoryResolutionGraph {
    val catalog: StoryPort
    val resolvers: Map<String, PlaybackItemResolver>
}
