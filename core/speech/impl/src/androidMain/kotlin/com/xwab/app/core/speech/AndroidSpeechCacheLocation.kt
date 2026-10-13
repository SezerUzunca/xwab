package com.xwab.app.core.speech

import android.content.Context
import dev.zacsweers.metro.Inject
import okio.Path
import okio.Path.Companion.toPath

/** Reads `cacheDir` only when a track is rendered: the getter can create the directory, so it is disk work. */
@Inject
internal class AndroidSpeechCacheLocation(
    private val context: Context,
) : SpeechCacheLocation {
    override fun root(): Path = context.cacheDir.resolve(SPEECH_DIRECTORY).absolutePath.toPath()

    private companion object {
        const val SPEECH_DIRECTORY = "speech"
    }
}
