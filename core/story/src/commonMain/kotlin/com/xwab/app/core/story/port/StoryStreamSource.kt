package com.xwab.app.core.story.port

/** The remote stream used to play one story. */
public data class StoryStreamSource(public val httpsUrl: String) {
    init {
        require(httpsUrl.startsWith("https://")) { "Story audio must stream over HTTPS." }
    }
}
