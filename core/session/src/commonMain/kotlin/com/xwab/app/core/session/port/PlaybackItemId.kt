package com.xwab.app.core.session.port

/**
 * What the session was asked to play: which kind of thing, and which one of them.
 *
 * A kind and a raw value rather than a catalog id, because the session holds one playback at a time
 * for the whole app and a sound is not the only thing that can occupy it. The kind is not
 * decoration: two content types are allowed to share a raw id — `forest` is a plausible name for
 * both a sound and a story — and without the kind the session would mistake one for the other and
 * skip the resolution that makes them different.
 *
 * Both halves are plain strings, and that is the whole reason a content type can be added or
 * removed without touching this module. [kind] is owned by the content module it names — see
 * `SOUND_PLAYBACK_KIND` and `STORY_PLAYBACK_KIND` — and this module holds no list of them: it
 * resolves whatever it is handed by looking the kind up among the resolvers contributed to the
 * application graph. A kind nothing has registered is reported as an item that could not be found,
 * which is what a removed content type looks like from here.
 *
 * It used to be a closed enum, which read as stronger typing and was: a screen could not invent a
 * kind, and every `when` over it was exhaustive. It cost the thing this app needs more — every new
 * content type edited this file, the engine id mapping beside it, and the session's constructor.
 * What the enum guaranteed is now checked where it actually matters: the composition root has a
 * test that every registered kind has a screen to open.
 */
public data class PlaybackItemId(
    public val kind: String,
    public val value: String,
) {
    init {
        require(kind.isNotBlank()) { "A playback item id needs a kind." }
        require(value.isNotBlank()) { "A playback item id cannot be blank." }
        // The engine id is `kind:value`, so a kind carrying the separator would not survive the
        // round trip through the platform player.
        require(':' !in kind) { "A playback kind cannot contain ':': $kind" }
    }
}
