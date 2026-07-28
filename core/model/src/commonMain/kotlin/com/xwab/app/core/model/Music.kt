package com.xwab.app.core.model

data class Music(
    val id: String,
    val name: String,
    val categoryId: String,
    val durationSeconds: Int,
    val playbackTitle: String = name,
    val playbackArtist: String = "Sleep Sounds",
) {
    val formattedDuration: String
        get() = formatDuration(durationSeconds)
}

fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
