package com.xwab.app.core.rehearsal

import com.xwab.app.core.rehearsal.port.REHEARSAL_PLAYBACK_KIND
import com.xwab.app.core.speech.port.NoiseBed
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechSegment

/**
 * What a rehearsal says: a short settling-in, the listener's own script a sentence at a time with
 * room to picture each one, then time to go through it again in their own mind.
 *
 * Imagery rehearsal is practised awake, so the frame neither promises sleep nor talks the listener
 * down into it; it ends by asking them to open their eyes. The framing lines were written for this
 * app.
 */
internal fun rehearsalRequest(script: String) = SpeechRequest(
    folder = REHEARSAL_PLAYBACK_KIND,
    // One name for every wording: speech replaces the old track when the words change.
    name = TRACK_NAME,
    languageTag = ENGLISH,
    segments = buildList {
        add(SpeechSegment.Pause(OPENING_SECONDS))
        OPENING.forEach { (text, pause) -> addLine(text, pause) }
        sentencesOf(script).forEach { addLine(it, SENTENCE_PAUSE_SECONDS) }
        CLOSING.forEach { (text, pause) -> addLine(text, pause) }
    },
    bed = REHEARSAL_BED,
    // The same words make the same track: only a changed script is spoken again.
    reuse = true,
)

private fun MutableList<SpeechSegment>.addLine(text: String, pauseSeconds: Double) {
    add(SpeechSegment.Spoken(text))
    add(SpeechSegment.Pause(pauseSeconds))
}

/**
 * The script as the voice should read it: a sentence at a time, so each gets its own pause. A
 * sentence ends at a full stop, question or exclamation mark followed by a space, or at a line
 * break; text without either is one sentence.
 */
internal fun sentencesOf(script: String): List<String> {
    val sentences = mutableListOf<String>()
    val current = StringBuilder()
    fun finish() {
        current.toString().trim().takeIf { it.isNotEmpty() }?.let(sentences::add)
        current.clear()
    }
    script.forEachIndexed { index, char ->
        if (char == '\n' || char == '\r') {
            finish()
        } else {
            current.append(char)
            val next = script.getOrNull(index + 1)
            if (char in SENTENCE_ENDS && (next == null || next.isWhitespace())) finish()
        }
    }
    finish()
    return sentences
}

/** The framing's words, with how long the listener has after each. */
private val OPENING = listOf(
    "Find a comfortable position, and let your eyes close if you like." to 5.0,
    "Take a slow breath in, and a long breath out." to 6.0,
    "You are going to rehearse the new version of your dream. " +
        "As each part is read, picture it as clearly as you can." to 4.0,
    "Here is your new dream." to 3.0,
)

private val CLOSING = listOf(
    "Now go through your new dream once more in your own mind, from the beginning to the end." to 45.0,
    "Notice how it ends, and how you feel when it does." to 8.0,
    "This is the version to return to whenever the old dream comes back." to 5.0,
    "Take a slow breath, and open your eyes when you are ready." to 3.0,
)

/** How long each of the listener's sentences is given to form as a picture. */
internal const val SENTENCE_PAUSE_SECONDS = 6.0

/** A moment of the bed alone before the first word. */
private const val OPENING_SECONDS = 3.0

private val SENTENCE_ENDS = setOf('.', '!', '?')

/** Softer than a guide's: the listener is awake and picturing, and the bed only fills the silences. */
private val REHEARSAL_BED = NoiseBed(level = 0.03, fadeSeconds = 6.0)

private const val TRACK_NAME = "own-dream"
private const val ENGLISH = "en-US"
