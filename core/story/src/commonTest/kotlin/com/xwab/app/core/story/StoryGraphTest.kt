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
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StoryGraphTest {
    /** The production module graph: one catalog, which the resolver reads. */
    @Test
    fun theModuleGraphResolvesAShippedStoryFromItsOwnCatalog() = runBlocking {
        val graph = createGraph<StoryGraph>()
        val story = graph.catalog.observeStories().first().first()

        assertEquals(storyManifest, graph.catalog.observeStories().first())
        assertEquals(story, graph.catalog.observeStory(story.id).first())
        assertSame(graph.catalog, graph.catalog)
        assertSame(graph.resolver, graph.resolver)
        val resolved = assertIs<ItemResolution.Resolved>(graph.resolver.resolve(story.id.value))
        assertEquals(story.title, resolved.title)
    }

    /** Exercises the contributed provider map consumed by session, including the bridge's scope. */
    @Test
    fun theInstalledResolverCanPlayEveryPublishedStory() = runBlocking {
        val graph = createGraph<StoryResolutionGraph>()
        assertEquals(setOf(STORY_PLAYBACK_KIND), graph.resolverProviders.keys)
        val publishedSources = storySourceManifest.associateBy(StorySource::itemId)
        val provider = graph.resolverProviders.getValue(STORY_PLAYBACK_KIND)
        val resolver = provider()
        assertSame(resolver, provider())

        graph.catalog.observeStories().first().forEach { story ->
            val resolved = assertIs<ItemResolution.Resolved>(resolver.resolve(story.id.value))
            assertEquals(publishedSources.getValue(story.id.value).httpsUrl, resolved.uri)
            assertEquals(story.title, resolved.title)
            assertEquals(story.title, resolved.displayName)
            assertEquals(story.narrator, resolved.artist)
            assertEquals(false, resolved.policy.looping)
        }
    }
}

@DependencyGraph(AppScope::class)
internal interface StoryResolutionGraph {
    val catalog: StoryPort
    val resolverProviders: Map<String, () -> PlaybackItemResolver>
}
