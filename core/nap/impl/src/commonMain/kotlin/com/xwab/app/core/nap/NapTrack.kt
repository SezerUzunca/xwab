package com.xwab.app.core.nap

import com.xwab.app.core.nap.port.NAP_PLAYBACK_KIND
import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.speech.port.NoiseBed
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechSegment

/**
 * What a nap says, over soft noise from start to end: one line to settle in, held for the whole
 * nap, then the wake-up from the nap's last second on.
 *
 * The opening line is held to the nap's length, so the wake-up starts exactly that long after play
 * does, whatever the voice's pace. The wake-up repeats a few times with short silences between, as
 * one line can be slept through. Nothing is measured: the length is fixed. The lines were written
 * for this app.
 */
internal fun napRequest(nap: Nap) = SpeechRequest(
    folder = NAP_PLAYBACK_KIND,
    name = nap.id.value,
    languageTag = nap.languageTag,
    segments = buildList {
        add(SpeechSegment.Spoken(settleLine(nap.minutes), minSeconds = nap.minutes * SECONDS_PER_MINUTE))
        repeat(WAKE_ROUNDS) { WAKE_UP.forEach { (text, seconds) -> add(SpeechSegment.Spoken(text, seconds)) } }
        add(SpeechSegment.Spoken(LAST_LINE))
        add(SpeechSegment.Pause(CLOSING_SECONDS))
    },
    bed = NAP_BED,
    // The same words and length every time: made once per voice, then handed back.
    reuse = true,
)

private fun settleLine(minutes: Int) =
    "Lie back and let yourself rest. You do not have to fall asleep; resting is enough. " +
        "The voice will come back in $minutes minutes."

/** One round of the wake-up, each line held long enough to be heard and the next to surprise. */
private val WAKE_UP = listOf(
    "It is time to wake up." to 6.0,
    "Take a deep breath, and stretch your arms and legs." to 8.0,
    "Open your eyes, and sit up slowly." to 8.0,
)

private const val LAST_LINE = "Let some light in, and have a glass of water before you go."

/** How many times the wake-up repeats: about a minute in all. */
internal const val WAKE_ROUNDS = 3

/** A short close after the last word, the bed fading out under it. */
private const val CLOSING_SECONDS = 5.0

private const val SECONDS_PER_MINUTE = 60.0

/** Soft enough to sleep under, loud enough to cover a quiet room's sounds. */
private val NAP_BED = NoiseBed(level = 0.035, fadeSeconds = 8.0)
