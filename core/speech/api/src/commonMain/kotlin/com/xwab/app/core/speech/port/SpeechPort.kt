package com.xwab.app.core.speech.port

/**
 * Speaks text with the device's own voice into a local audio track a platform player can open.
 *
 * For content capabilities, not screens: a deck of shuffle words or a relaxation guide is content
 * that a module describes and this one voices, much as sound content is downloaded by delivery.
 * Nothing is sent anywhere and no voice ships with the app; each platform's text-to-speech speaks.
 */
interface SpeechPort {
    /**
     * Renders [request] and answers where the track is, or why it could not be made. Cancellation
     * propagates: a caller that leaves stops the render.
     */
    suspend fun render(request: SpeechRequest): SpeechResult
}

/**
 * One track to speak.
 *
 * @param folder the caller's own directory for its tracks, so two content modules cannot overwrite
 *   each other's. A content module passes its playback kind.
 * @param name what the track is, within [folder]: a deck or guide id. A new track of the same name
 *   replaces the old one.
 * @param languageTag the BCP 47 tag of the voice to speak with, such as `en-US`.
 * @param bed a quiet noise under the whole track, or none.
 * @param reuse whether a track already made from the same segments with the same voice is returned
 *   as it is. True for fixed content, such as a guide; false where every render should differ, such
 *   as shuffle words drawn in a new order.
 */
data class SpeechRequest(
    val folder: String,
    val name: String,
    val languageTag: String,
    val segments: List<SpeechSegment>,
    val bed: NoiseBed? = null,
    val reuse: Boolean = false,
) {
    init {
        require(FILE_NAME.matches(folder)) { "A speech folder is lower-case letters, digits and dashes: $folder" }
        require(FILE_NAME.matches(name)) { "A speech track name is lower-case letters, digits and dashes: $name" }
        require(languageTag.isNotBlank()) { "A speech request needs a voice language." }
        require(segments.any { it is SpeechSegment.Spoken }) { "A speech track needs something to say." }
    }

    companion object {
        /** Safe as a file name on every platform, and never mistaken for a path. */
        val FILE_NAME = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    }
}

/** One stretch of a track, in order. */
sealed interface SpeechSegment {
    /**
     * [text] in the device's voice, brought to the same moderate level as everything else spoken.
     *
     * @param minSeconds how long the segment lasts at least, from its start: what the voice leaves
     *   of it is silence. Zero is the speech alone.
     */
    data class Spoken(val text: String, val minSeconds: Double = 0.0) : SpeechSegment {
        init {
            require(text.isNotBlank()) { "Nothing to say." }
            require(minSeconds >= 0.0) { "A segment cannot last less than nothing: $minSeconds" }
        }
    }

    /** Silence, or only the bed if the track has one. */
    data class Pause(val seconds: Double) : SpeechSegment {
        init {
            require(seconds > 0.0) { "A pause lasts some time: $seconds" }
        }
    }
}

/**
 * Soft brown noise under a track: it masks a day's household and street sounds while the voice
 * speaks, and keeps the silences between instructions from sounding like a dropped connection.
 *
 * @param level its loudness as a share of full scale, well under the voice.
 * @param fadeSeconds how long it takes to rise at the start and fall at the end.
 */
data class NoiseBed(val level: Double, val fadeSeconds: Double) {
    init {
        require(level > 0.0 && level <= MAX_LEVEL) { "A bed stays under the voice: $level" }
        require(fadeSeconds >= 0.0) { "A fade cannot be negative: $fadeSeconds" }
    }

    companion object {
        /** A quarter of the voice's peak: under it, never level with it. */
        const val MAX_LEVEL = 0.1
    }
}

/** What a render came to. */
sealed interface SpeechResult {
    /** @param path the track's absolute path, which both platform players open as a local file. */
    data class Rendered(val path: String) : SpeechResult

    /** No voice for the language, or no room to write the track. Worth another try later. */
    data class Unavailable(val reason: String) : SpeechResult
}
