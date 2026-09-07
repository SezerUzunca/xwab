package com.xwab.app.core.storysource.port

import com.xwab.app.core.story.port.StoryId

/** The remote stream used to play one story. */
public data class StoryStreamSource(public val httpsUrl: String) {
    init {
        require(httpsUrl.startsWith("https://")) { "Story audio must stream over HTTPS." }
    }
}

/** Supplies story streams without exposing the shipped manifest. */
public interface StorySourcePort {
    /** The source for [storyId], or `null` when the catalog has no such story. */
    public fun sourceFor(storyId: StoryId): StoryStreamSource?
}
