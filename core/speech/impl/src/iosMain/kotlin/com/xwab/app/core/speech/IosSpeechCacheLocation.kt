@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.speech

import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The app's caches directory, looked up when a track is rendered rather than when the graph is built. */
@Inject
internal class IosSpeechCacheLocation(
    private val fileManager: NSFileManager,
) : SpeechCacheLocation {
    override fun root(): Path = (
        requireNotNull(
            fileManager.URLForDirectory(
                directory = NSCachesDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path,
        ) + "/$SPEECH_DIRECTORY"
    ).toPath()

    private companion object {
        const val SPEECH_DIRECTORY = "speech"
    }
}
