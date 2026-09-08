package com.xwab.app.core.session.port

/** What kind of thing is being played. One resolver in this module answers for each. */
public enum class PlaybackKind {
    SOUND,
    STORY,
}

/**
 * What the session was asked to play.
 *
 * A kind and a raw value rather than a `TrackId`, because the session holds one playback at a time
 * for the whole app and a sound is not the only thing that can occupy it. The kind is not
 * decoration: a sound and a story are allowed to share a raw id — `forest` is a plausible name for
 * both — and without the kind the session would mistake one for the other and skip the resolution
 * that makes them different.
 *
 * The value stays a `String` on purpose. Making it `TrackId` would put the sound catalog back in
 * this module's public API, which is exactly the coupling this type exists to remove; a screen
 * converts at the one line where it calls [PlaybackPort.play].
 */
public data class PlaybackItemId(
    public val kind: PlaybackKind,
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "A playback item id cannot be blank." }
    }

    public companion object {
        public fun sound(value: String): PlaybackItemId = PlaybackItemId(PlaybackKind.SOUND, value)

        public fun story(value: String): PlaybackItemId = PlaybackItemId(PlaybackKind.STORY, value)
    }
}
