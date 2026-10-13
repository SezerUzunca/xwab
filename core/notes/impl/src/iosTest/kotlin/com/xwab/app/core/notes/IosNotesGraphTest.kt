package com.xwab.app.core.notes

import com.xwab.app.core.notes.port.NoteSaveResult
import dev.zacsweers.metro.createGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSFileManager

/** The iOS production graph, with its notes file built from the system's file manager. */
class IosNotesGraphTest {
    @Test
    fun theFileLivesInTheDocumentsDirectory() {
        val path = IosNotesFile(NSFileManager.defaultManager).path().toString()

        assertTrue(path.endsWith("Documents/$NOTES_FILE_NAME"), path)
    }

    /**
     * Nothing replaced: the graph's own file and scope write a note to disk and read it back. The
     * simulator keeps its documents between runs, so the test removes what it wrote.
     */
    @Test
    fun theUnmodifiedGraphWritesANoteAndReadsItBack() = runBlocking {
        val graph = createGraph<IosNotesGraph>()
        assertSame(graph.notes, graph.notes)
        try {
            assertEquals(NoteSaveResult.Saved, graph.notes.save(KEY, "Call the pharmacy"))

            val read = withTimeout(TIMEOUT_MS) { graph.notes.observe(KEY).first { it.isNotEmpty() } }
            assertEquals("Call the pharmacy", read)
        } finally {
            graph.notes.save(KEY, "")
        }
    }

    private companion object {
        const val KEY = "graph-test"
        const val TIMEOUT_MS = 10_000L
    }
}
