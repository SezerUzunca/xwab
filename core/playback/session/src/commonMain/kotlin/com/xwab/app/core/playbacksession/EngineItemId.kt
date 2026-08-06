package com.xwab.app.core.playbacksession

/**
 * How a [PlaybackItemId] travels through `core:playback:engine`, which identifies a source by plain
 * string and knows nothing about kinds.
 *
 * The prefix is what stops a sound and a story with the same raw id from looking like one source to
 * the engine — and to the session, which compares the engine's current id against what was asked
 * for and would otherwise send `Play` for a story while a sound of that name is attached.
 *
 * The prefixes are written out rather than derived from the enum name, because they leave this
 * process: on Android the playback service outlives the app, so a reconnect reads back ids that an
 * earlier build wrote. Renaming a constant must not change what those mean.
 */
private const val SOUND_PREFIX = "sound"
private const val STORY_PREFIX = "story"

internal fun PlaybackItemId.toEngineId(): String = "${kind.prefix()}:$value"

/**
 * The item an engine source id names, or `null` when it names nothing.
 *
 * An id with no known prefix is read as a sound. That is the upgrade path, not a guess: a media
 * service still running from a build that wrote bare track ids reconnects into this one, and
 * treating `gentle-rain` as a sound is what keeps its playback attached instead of silently
 * reloading it.
 */
internal fun playbackItemIdOf(engineId: String): PlaybackItemId? {
    if (engineId.isBlank()) return null

    val prefix = engineId.substringBefore(':', missingDelimiterValue = "")
    val value = engineId.substringAfter(':', missingDelimiterValue = "")
    val kind = when (prefix) {
        SOUND_PREFIX -> PlaybackKind.SOUND
        STORY_PREFIX -> PlaybackKind.STORY
        else -> return PlaybackItemId(PlaybackKind.SOUND, engineId)
    }

    return if (value.isBlank()) null else PlaybackItemId(kind, value)
}

private fun PlaybackKind.prefix(): String = when (this) {
    PlaybackKind.SOUND -> SOUND_PREFIX
    PlaybackKind.STORY -> STORY_PREFIX
}
