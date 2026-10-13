@file:OptIn(ExperimentalKermitApi::class)

package com.xwab.app.core.notes

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.TestConfig
import co.touchlab.kermit.TestLogWriter
import com.xwab.app.core.notes.port.NoteSaveResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.IOException

class DataStoreNotesAdapterTest {
    @Test
    fun aNoteIsKeptUnderItsKeyAndReplacedWhole() = runTest {
        val store = InMemoryPreferences()
        val notes = DataStoreNotesAdapter(store, SilentLogger)

        assertEquals("", notes.observe("after-shift").first(), "nothing written yet reads as empty")
        assertEquals(NoteSaveResult.Saved, notes.save("after-shift", "Call the bank\nBuy milk"))
        notes.save("other", "Something else")

        val reopened = DataStoreNotesAdapter(store, SilentLogger)
        assertEquals("Call the bank\nBuy milk", reopened.observe("after-shift").first())
        notes.save("after-shift", "Buy milk")
        assertEquals("Buy milk", notes.observe("after-shift").first())
        assertEquals("Something else", notes.observe("other").first())
    }

    @Test
    fun aBlankNoteRemovesIt() = runTest {
        val store = InMemoryPreferences()
        val notes = DataStoreNotesAdapter(store, SilentLogger)
        notes.save("after-shift", "Buy milk")

        notes.save("after-shift", "   ")

        assertEquals("", notes.observe("after-shift").first())
        assertEquals(emptyPreferences(), store.data.first())
    }

    /** A disk that refuses the write is reported, and logged, so the screen can keep the text. */
    @Test
    fun aRefusedWriteIsReportedAndLogged() = runTest {
        val log = TestLogWriter(loggable = Severity.Verbose)
        val notes = DataStoreNotesAdapter(InMemoryPreferences(failWrites = true), recordingLogger(log))

        assertEquals(NoteSaveResult.Unavailable, notes.save("after-shift", "Buy milk"))
        assertEquals(Severity.Error, log.logs.single().severity)
    }

    /** An unreadable file shows as an empty note rather than a crash. */
    @Test
    fun anUnreadableNoteReadsAsEmpty() = runTest {
        val notes = DataStoreNotesAdapter(UnreadablePreferences, SilentLogger)

        assertEquals("", notes.observe("after-shift").first())
    }

    @Test
    fun keysAreNamesOnDisk() = runTest {
        val notes = DataStoreNotesAdapter(InMemoryPreferences(), SilentLogger)

        assertFailsWith<IllegalArgumentException> { notes.save("After Shift", "x") }
        assertFailsWith<IllegalArgumentException> { notes.observe("../notes") }
    }

    private class InMemoryPreferences(private val failWrites: Boolean = false) : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("The disk is full.")
            return transform(state.value).also { state.value = it }
        }
    }

    private object UnreadablePreferences : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("The file is corrupt.") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("The file is corrupt.")
    }
}

private val SilentLogger = Logger(StaticConfig(logWriterList = emptyList()))

private fun recordingLogger(writer: TestLogWriter): Logger =
    Logger(TestConfig(minSeverity = Severity.Verbose, logWriterList = listOf(writer)))
