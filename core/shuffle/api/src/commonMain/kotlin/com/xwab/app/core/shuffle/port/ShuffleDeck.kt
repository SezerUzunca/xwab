package com.xwab.app.core.shuffle.port

// Explicit because this is common code: `kotlin.jvm.*` is a default import on the JVM only, so
// Android compiles this file without the import and the Kotlin/Native targets do not.
import kotlin.jvm.JvmInline

/**
 * The identity of one word deck.
 *
 * A type of its own rather than a bare `String`, as story and track ids are: it travels through
 * screen state and into a playback item id, both of which carry other strings too.
 *
 * It refuses a blank value. An empty id is a deck that can be listed and never played.
 */
@JvmInline
value class ShuffleDeckId(val value: String) {
    init {
        require(value.isNotBlank()) { "A shuffle deck id cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * One cognitive-shuffle deck, as a screen needs to know it: what it is called and how it is spoken.
 *
 * The words themselves are not here. Which words a deck holds and in what order they are spoken is
 * this module's business, like a sound's stream address; a screen plays the deck through the
 * session and never sees them.
 *
 * @param languageTag the BCP 47 tag of the voice that speaks the words, such as `en-US`. The device
 *   supplies the voice, so a screen can say which language it needs.
 * @param wordIntervalSeconds how far apart the words start. Eight seconds is the pace of the
 *   serial diverse imagining studies: long enough to picture a word, too short to start thinking
 *   about it.
 */
data class ShuffleDeck(
    val id: ShuffleDeckId,
    val title: String,
    val description: String,
    val languageTag: String,
    val wordIntervalSeconds: Int,
) {
    init {
        require(title.isNotBlank()) { "A shuffle deck needs a title: ${id.value}" }
        require(description.isNotBlank()) { "A shuffle deck needs a description: ${id.value}" }
        require(languageTag.isNotBlank()) { "A shuffle deck needs a voice language: ${id.value}" }
        require(wordIntervalSeconds in MIN_WORD_INTERVAL_SECONDS..MAX_WORD_INTERVAL_SECONDS) {
            "A shuffle deck speaks a word every $MIN_WORD_INTERVAL_SECONDS to $MAX_WORD_INTERVAL_SECONDS " +
                "seconds: ${id.value} asks for $wordIntervalSeconds"
        }
    }

    // Public like everything in a port package, which `checkArchitecture` requires of every
    // declaration here, nested ones included.
    companion object {
        /** Shorter leaves no time to picture a word before the next arrives. */
        const val MIN_WORD_INTERVAL_SECONDS = 3

        /** Longer leaves room for the thoughts the words are meant to crowd out. */
        const val MAX_WORD_INTERVAL_SECONDS = 30
    }
}
