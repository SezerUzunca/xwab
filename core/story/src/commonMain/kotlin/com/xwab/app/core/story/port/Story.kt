package com.xwab.app.core.story.port

/**
 * One sleep story, as a screen needs to know it.
 *
 * Metadata only: what it is called, who wrote and reads it, and how long it runs.
 * StoryPort supplies metadata only; the playback session resolves the stream through SourcePort.
 *
 * The invariants are checked here rather than trusted. A story with no title or author is a row a
 * screen cannot explain; a negative duration is a progress bar that cannot be drawn.
 */
data class Story(
    val id: StoryId,
    val title: String,
    val author: String,
    val description: String,
    val narrator: String?,
    val durationSeconds: Int,
    val artworkUrl: String?,
) {
    init {
        require(title.isNotBlank()) { "A story needs a title: ${id.value}" }
        require(author.isNotBlank()) { "A story needs an author: ${id.value}" }
        require(description.isNotBlank()) { "A story needs a description: ${id.value}" }
        require(durationSeconds > 0) { "A story needs a positive duration: ${id.value}" }
        require(narrator == null || narrator.isNotBlank()) {
            "A story's narrator is either absent or named, never blank: ${id.value}"
        }
        require(artworkUrl == null || artworkUrl.startsWith("https://")) {
            "Story artwork must use HTTPS: ${id.value}"
        }
    }
}
