package com.xwab.app.core.shuffle

import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import com.xwab.app.core.speech.port.SpeechPort
import com.xwab.app.core.speech.port.SpeechRequest
import com.xwab.app.core.speech.port.SpeechResult

internal fun deck(id: String = TEST_DECK, intervalSeconds: Int = 3) = ShuffleDeck(
    id = ShuffleDeckId(id),
    title = "Test Deck",
    description = "Words for a test.",
    languageTag = "en-US",
    wordIntervalSeconds = intervalSeconds,
)

internal fun wordList(
    words: List<String> = listOf("kite", "cup", "owl", "pear"),
    wordsPerTrack: Int = 3,
    deckId: String = TEST_DECK,
) = ShuffleWordList(deckId = deckId, wordsPerTrack = wordsPerTrack, words = words)

internal const val TEST_DECK = "test-deck"

/** A speech port that records what it was asked and answers with a made-up path, or not at all. */
internal class FakeSpeech(private val unavailable: String? = null) : SpeechPort {
    val requests = mutableListOf<SpeechRequest>()

    override suspend fun render(request: SpeechRequest): SpeechResult {
        requests += request
        return unavailable?.let(SpeechResult::Unavailable)
            ?: SpeechResult.Rendered("/cache/speech/tracks/${request.folder}/${request.name}.${requests.size}.wav")
    }
}
