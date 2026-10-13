@file:OptIn(PlaybackResolverApi::class, ExperimentalKermitApi::class)

package com.xwab.app.core.shuffle

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackResolverApi
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import co.touchlab.kermit.Logger
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class ShufflePlaybackResolverTest {
    private val fileSystem = FakeFileSystem()

    @AfterTest
    fun closeFileSystem() {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }

    @Test
    fun aDeckBecomesALoopingLocalTrackUnderItsOwnName() = runTest {
        val resolution = resolver().resolve(TEST_DECK)

        val resolved = assertIs<ItemResolution.Resolved>(resolution)
        assertTrue(resolved.uri.startsWith("/cache/shuffle/tracks/"), resolved.uri)
        assertTrue(fileSystem.exists(resolved.uri.toPath()), "the players open the file the resolver names")
        assertEquals("Test Deck", resolved.title)
        assertEquals("Test Deck", resolved.displayName)
        assertNull(resolved.artist)
        assertTrue(resolved.policy.looping, "a track repeats until the sleep timer stops it")
    }

    @Test
    fun anUnknownDeckIsNotFound() = runTest {
        assertEquals(ItemResolution.NotFound, resolver().resolve("no-such-deck"))
    }

    /** No voice for the deck's language is worth another tap once one is installed, and a log line. */
    @Test
    fun aDeckTheDeviceCannotSpeakIsUnavailableAndLogged() = runTest {
        val log = TestLogWriter(loggable = Severity.Verbose)

        val speechless = resolver(voice = FakeVoice(fileSystem, available = false), logger = recordingLogger(log))

        assertIs<ItemResolution.Unavailable>(speechless.resolve(TEST_DECK))
        assertEquals(Severity.Warn, log.logs.single().severity)
    }

    @Test
    fun aDeckWithoutWordsIsUnavailable() = runTest {
        assertIs<ItemResolution.Unavailable>(resolver(wordLists = emptyList()).resolve(TEST_DECK))
    }

    @Test
    fun aDeckCanHaveOnlyOneWordList() = runTest {
        assertFailsWith<IllegalArgumentException> { resolver(wordLists = listOf(wordList(), wordList())) }
    }

    private fun TestScope.resolver(
        voice: WordVoice = FakeVoice(fileSystem),
        wordLists: List<ShuffleWordList> = listOf(wordList()),
        logger: Logger = SilentLogger,
    ) = ShufflePlaybackResolver(
        catalog = ManifestShuffleCatalogAdapter(listOf(deck())),
        wordLists = wordLists,
        renderer = ShuffleTrackRenderer(
            voice = voice,
            location = { "/cache/shuffle".toPath() },
            fileSystem = fileSystem,
            fileDispatcher = StandardTestDispatcher(testScheduler),
            random = Random(1),
        ),
        logger = logger,
    )
}
