package com.xwab.app.core.shuffle

import com.xwab.app.core.shuffle.port.ShuffleDeck
import com.xwab.app.core.shuffle.port.ShuffleDeckId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ShuffleDeckTest {
    @Test
    fun aDeckIdCannotBeBlank() {
        assertFailsWith<IllegalArgumentException> { ShuffleDeckId(" ") }
        assertEquals("kite", ShuffleDeckId("kite").toString())
    }

    @Test
    fun aDeckNeedsANameADescriptionAndAVoice() {
        assertFailsWith<IllegalArgumentException> { deck().copy(title = " ") }
        assertFailsWith<IllegalArgumentException> { deck().copy(description = "") }
        assertFailsWith<IllegalArgumentException> { deck().copy(languageTag = "") }
    }

    /** Too fast leaves no time to picture a word; too slow leaves room for the day's thoughts. */
    @Test
    fun aDeckSpeaksAtAPaceTheTechniqueAllows() {
        assertFailsWith<IllegalArgumentException> { deck(intervalSeconds = ShuffleDeck.MIN_WORD_INTERVAL_SECONDS - 1) }
        assertFailsWith<IllegalArgumentException> { deck(intervalSeconds = ShuffleDeck.MAX_WORD_INTERVAL_SECONDS + 1) }
        deck(intervalSeconds = ShuffleDeck.MIN_WORD_INTERVAL_SECONDS)
        deck(intervalSeconds = ShuffleDeck.MAX_WORD_INTERVAL_SECONDS)
    }

    @Test
    fun theCatalogFindsDecksByIdAndRefusesDuplicates() = runTest {
        val catalog = ManifestShuffleCatalogAdapter(listOf(deck("a"), deck("b")))

        assertEquals(listOf("a", "b"), catalog.observeDecks().first().map { it.id.value })
        assertEquals("b", catalog.observeDeck(ShuffleDeckId("b")).first()?.id?.value)
        assertEquals(null, catalog.observeDeck(ShuffleDeckId("c")).first())
        assertFailsWith<IllegalArgumentException> { ManifestShuffleCatalogAdapter(listOf(deck("a"), deck("a"))) }
    }
}
