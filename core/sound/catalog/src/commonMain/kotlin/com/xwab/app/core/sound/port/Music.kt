package com.xwab.app.core.sound.port

/**
 * One catalog track, as a screen needs to know it.
 *
 * The invariants are checked here rather than trusted, the same way the story catalog checks its
 * own model. A track with no name is a row a screen would draw blank; a duration that is not
 * positive is a label that reads "0:00" for something that plays. The manifest is hand-written
 * today and a feed may write it later, and both go through this.
 */
public data class Music(
    public val id: TrackId,
    public val name: String,
    public val categoryId: CategoryId,
    public val durationSeconds: Int,
    public val playbackTitle: String = name,
    public val playbackArtist: String = "Sleep Sounds",
) {
    init {
        require(name.isNotBlank()) { "A track needs a name: ${id.value}" }
        // `CategoryId` already refuses a blank, so a track only has to hold one at all.
        require(durationSeconds > 0) { "A track needs a positive duration: ${id.value}" }
        require(playbackTitle.isNotBlank()) { "A track's playback title cannot be blank: ${id.value}" }
        require(playbackArtist.isNotBlank()) { "A track's playback artist cannot be blank: ${id.value}" }
    }
}
