package com.xwab.app.core.story

import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StoryPortImplTest {
    private val night = story("night-came-slowly")
    private val idleFellow = story("an-idle-fellow")
    private val adapter = StoryPortImpl(entries = listOf(night, idleFellow).map(::entry))

    @Test
    fun theWholeManifestIsServedAsGiven() = runBlocking {
        assertEquals(listOf(night, idleFellow), adapter.observeStories().first())
    }

    @Test
    fun oneStoryIsServedById() = runBlocking {
        assertEquals(idleFellow, adapter.observeStory(StoryId("an-idle-fellow")).first())
    }

    @Test
    fun anUnknownIdEmitsNothingRatherThanNeverEmitting() = runBlocking {
        assertNull(adapter.observeStory(StoryId("no-such-story")).first())
    }

    @Test
    fun twoStoriesUnderOneIdAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            StoryPortImpl(
                entries = listOf(night, story("night-came-slowly", title = "Another Version")).map(::entry),
            )
        }
    }

    @Test
    fun theShippedManifestIsServed() = runBlocking {
        val shippedAdapter = StoryPortImpl()

        assertEquals(storyCatalog, shippedAdapter.observeStories().first())
        assertEquals(
            storyCatalog.first(),
            shippedAdapter.observeStory(storyCatalog.first().id).first(),
        )
    }

    private fun entry(story: Story) = StoryEntry(story, "https://example.test/${story.id.value}.mp3")

    private fun story(id: String, title: String = id) = Story(
        id = StoryId(id),
        title = title,
        author = "Kate Chopin",
        description = "A literary short story.",
        narrator = "Alan Davis Drake",
        durationSeconds = 600,
        artworkUrl = null,
    )
}
