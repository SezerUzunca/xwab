package com.xwab.app.core.story

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StoryIdTest {
    @Test
    fun theIdIsTheStringItWasGiven() {
        assertEquals("night-came-slowly", StoryId("night-came-slowly").value)
        assertEquals("night-came-slowly", StoryId("night-came-slowly").toString())
    }

    /**
     * A blank id would list a story that no `observeStory` call could ever match, so it is refused
     * where it is built rather than where it fails to open.
     */
    @Test
    fun aBlankIdIsRejected() {
        assertFailsWith<IllegalArgumentException> { StoryId("") }
        assertFailsWith<IllegalArgumentException> { StoryId("   ") }
    }
}
