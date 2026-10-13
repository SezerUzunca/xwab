@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.notes

import com.xwab.app.core.notes.port.NotesPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** The notes file in the app's documents directory, looked up when DataStore first asks. */
@Inject
internal class IosNotesFile(
    private val fileManager: NSFileManager,
) : NotesFile {
    override fun path(): Path {
        val documentDirectory: NSURL? = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return (requireNotNull(documentDirectory).path + "/$NOTES_FILE_NAME").toPath()
    }
}

/** This module's own graph on iOS. It needs nothing from the platform beyond the system's file manager. */
@DependencyGraph(NotesScope::class, bindingContainers = [NotesBindings::class])
internal interface IosNotesGraph : NotesGraph {
    @Binds val IosNotesFile.bindFile: NotesFile

    @Provides
    fun provideFileManager(): NSFileManager = NSFileManager.defaultManager
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosNotesGraphAdapter : NotesPort by createGraph<IosNotesGraph>().notes
