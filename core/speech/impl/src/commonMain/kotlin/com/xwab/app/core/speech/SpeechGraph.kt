package com.xwab.app.core.speech

import co.touchlab.kermit.Logger
import com.xwab.app.core.speech.port.SpeechPort
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
import okio.FileSystem

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object SpeechScope

/**
 * The renderer and what it runs on: the same on every platform. Only the voice and the cache
 * location differ, and each platform's graph binds its own.
 */
@BindingContainer
internal interface SpeechBindings {
    @Binds val SpeechRendererAdapter.bindSpeech: SpeechPort

    companion object {
        @Provides fun provideFileSystem(): FileSystem = FileSystem.SYSTEM

        /** The renderer's file and voice work, off the thread that asked for the track. */
        @Provides fun provideFileDispatcher(): CoroutineDispatcher = Dispatchers.IO

        /** Fresh track names and the bed's noise; a test hands a seeded one. */
        @Provides fun provideRandom(): Random = Random.Default

        /**
         * Kermit's logger, whose level the application sets before this graph is built. Each class
         * tags its own; a test hands its class one that records what it writes.
         */
        @Provides fun provideLogger(): Logger = Logger
    }
}

/** What every platform's speech graph hands out. Only [SpeechPort] reaches the application graph. */
internal interface SpeechGraph {
    val speech: SpeechPort
}
