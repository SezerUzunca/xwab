package com.xwab.app.core.notes

import android.content.Context
import com.xwab.app.core.notes.port.NotesPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import okio.Path
import okio.Path.Companion.toPath

/** Reads `filesDir` only when DataStore asks: the getter can create the directory, so it is disk work. */
@Inject
internal class AndroidNotesFile(
    private val context: Context,
) : NotesFile {
    override fun path(): Path = context.filesDir.resolve(NOTES_FILE_NAME).absolutePath.toPath()
}

/**
 * This module's own graph on Android. It takes the [Context] rather than the files directory, so
 * nothing reads that directory until DataStore first opens the file, off the main thread.
 */
@DependencyGraph(NotesScope::class, bindingContainers = [NotesBindings::class])
internal interface AndroidNotesGraph : NotesGraph {
    @Binds val AndroidNotesFile.bindFile: NotesFile

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidNotesGraph
    }
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidNotesGraphAdapter(
    context: Context,
) : NotesPort by createGraphFactory<AndroidNotesGraph.Factory>()
    .create(context)
    .notes
