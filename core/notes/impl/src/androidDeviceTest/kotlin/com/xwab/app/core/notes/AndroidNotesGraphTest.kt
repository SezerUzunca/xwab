package com.xwab.app.core.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xwab.app.core.notes.port.NoteSaveResult
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** The Android production graph, with the Context only a device has. */
class AndroidNotesGraphTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Nothing replaced: the graph's own file and scope write a note to disk and read it back. */
    @Test
    fun theUnmodifiedGraphWritesANoteToDiskAndReadsItBack() = runBlocking {
        val graph = createGraphFactory<AndroidNotesGraph.Factory>().create(context)
        assertSame(graph.notes, graph.notes)
        try {
            assertEquals(NoteSaveResult.Saved, graph.notes.save(KEY, "Call the pharmacy"))

            val read = withTimeout(TIMEOUT_MS) { graph.notes.observe(KEY).first { it.isNotEmpty() } }
            assertEquals("Call the pharmacy", read)
            val file = context.filesDir.resolve(NOTES_FILE_NAME)
            assertTrue(file.length() > 0L, "nothing was written to ${file.absolutePath}")
        } finally {
            graph.notes.save(KEY, "")
        }
    }

    private companion object {
        const val KEY = "graph-test"
        const val TIMEOUT_MS = 10_000L
    }
}
