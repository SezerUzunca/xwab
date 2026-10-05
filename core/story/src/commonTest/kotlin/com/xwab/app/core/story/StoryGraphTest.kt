@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.story

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
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

        assertSame(graph.catalog, graph.catalog)
        val resolved = assertIs<ItemResolution.Resolved>(graph.resolver.resolve(story.id.value))
        assertEquals(story.title, resolved.title)
    }
}
