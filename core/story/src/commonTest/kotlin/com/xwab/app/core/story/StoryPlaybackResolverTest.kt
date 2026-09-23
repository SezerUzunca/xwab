@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.story

import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class StoryPlaybackResolverTest {
    private val story = storyManifest.first()
    private val catalog = ManifestStoryCatalogAdapter(listOf(story))
    private val source = StorySource(story.id.value, "https://example.test/story.mp3")

    @Test
    fun resolvesStreamMetadataAndNonLoopingPolicy() = runBlocking {
        val resolver = StoryPlaybackResolver(catalog, listOf(source))

        assertEquals(
            ItemResolution.Resolved(
                uri = source.httpsUrl,
                title = story.title,
                displayName = story.title,
                artist = story.narrator,
                policy = PlaybackPolicy(defaultLooping = false),
            ),
            resolver.resolve(story.id.value),
        )
    }

    @Test
    fun anUnknownCatalogIdIsNotFoundEvenWhenAnAddressExists() = runBlocking {
        val resolver = StoryPlaybackResolver(catalog, listOf(StorySource("unknown", source.httpsUrl)))

        assertEquals(ItemResolution.NotFound, resolver.resolve("unknown"))
    }

    @Test
    fun publishedMetadataWithoutAPhysicalSourceIsUnavailable() = runBlocking<Unit> {
        val resolver = StoryPlaybackResolver(catalog, emptyList())

        assertIs<ItemResolution.Unavailable>(resolver.resolve(story.id.value))
    }

    @Test
    fun duplicatePhysicalSourceIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            StoryPlaybackResolver(catalog, listOf(source, source.copy(httpsUrl = "https://example.test/other.mp3")))
        }
    }
}
