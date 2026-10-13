package com.xwab.app.core.shuffle

/**
 * The words one deck draws from, private to this module like a sound's stream address.
 *
 * The words are what the technique is about, so the rules for them are checked here rather than
 * trusted: lower-case letters, at most two words to an entry, each entry once. A track takes
 * [wordsPerTrack] of them without repeating one, which is why the list must be at least that long.
 *
 * @param wordsPerTrack how many words one rendered track speaks before it loops. It bounds the
 *   track's size on disk: each word is [com.xwab.app.core.shuffle.port.ShuffleDeck.wordIntervalSeconds]
 *   of uncompressed audio.
 */
internal data class ShuffleWordList(
    val deckId: String,
    val wordsPerTrack: Int,
    val words: List<String>,
) {
    init {
        require(deckId.isNotBlank()) { "A word list names the deck it belongs to." }
        require(wordsPerTrack >= MIN_WORDS_PER_TRACK) {
            "A track of fewer than $MIN_WORDS_PER_TRACK words is not a shuffle: $deckId"
        }
        require(words.size >= wordsPerTrack) {
            "$deckId speaks $wordsPerTrack words a track without repeating one, but lists ${words.size}."
        }
        val malformed = words.filterNot(WORD::matches)
        require(malformed.isEmpty()) { "$deckId has words that are not lower-case letters: $malformed" }
        val duplicates = words.groupBy { it }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "$deckId lists words more than once: $duplicates" }
    }

    private companion object {
        const val MIN_WORDS_PER_TRACK = 2

        /** "kite" or "pine cone": nothing a file name or a voice could stumble on. */
        val WORD = Regex("^[a-z]+( [a-z]+)?$")
    }
}
