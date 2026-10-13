package com.xwab.app.core.guide

/**
 * What one guide says, private to this module as a sound's address is.
 *
 * Each line is spoken, then followed by its pause: the time the listener has to do what it asks.
 * In a muscle-relaxation guide that is how long a muscle is held and how long it rests; in an
 * imagery guide, how long a picture is given to form.
 */
internal data class GuideScript(val guideId: String, val lines: List<GuideLine>) {
    init {
        require(guideId.isNotBlank()) { "A script names the guide it belongs to." }
        require(lines.isNotEmpty()) { "$guideId has nothing to say." }
    }
}

internal data class GuideLine(val text: String, val pauseSeconds: Double) {
    init {
        require(text.isNotBlank()) { "A line says something." }
        require(pauseSeconds > 0.0) { "Every line leaves time to follow it: \"$text\"" }
    }
}

/** Shorthand that keeps a script readable as a script: the line, then the seconds after it. */
internal fun line(text: String, pauseSeconds: Int) = GuideLine(text, pauseSeconds.toDouble())
