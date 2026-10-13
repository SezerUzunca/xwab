package com.xwab.app.core.notes

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.xwab.app.core.notes.port.NotesPort
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import okio.Path

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object NotesScope

/** The notes file's name: a file of its own, since DataStore refuses two instances over one file. */
internal const val NOTES_FILE_NAME = "notes.preferences_pb"

/**
 * Where the platform keeps the notes file. Each platform's graph binds its own. DataStore asks on
 * first access, off the main thread, so building the graph touches no file system.
 */
internal fun interface NotesFile {
    fun path(): Path
}

/**
 * The adapter and the store behind it: the same on every platform. Only the notes file differs, and
 * each platform's graph binds its own.
 */
@BindingContainer
internal interface NotesBindings {
    @Binds val DataStoreNotesAdapter.bindNotes: NotesPort

    companion object {
        /** Where DataStore reads and writes the file; it lives as long as the graph, so the application. */
        @Provides
        @SingleIn(NotesScope::class)
        fun provideDataStoreScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /** One store per file: DataStore refuses a second instance over the same path. */
        @Provides
        @SingleIn(NotesScope::class)
        fun provideDataStore(file: NotesFile, scope: CoroutineScope): DataStore<Preferences> =
            PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = file::path)

        /**
         * Kermit's logger, whose level the application sets before this graph is built. Each class
         * tags its own; a test hands its class one that records what it writes.
         */
        @Provides
        fun provideLogger(): Logger = Logger
    }
}

/** What every platform's notes graph hands out. Only [NotesPort] reaches the application graph. */
internal interface NotesGraph {
    val notes: NotesPort
}
