package com.xwab.app.core.speech

import com.xwab.app.core.speech.port.SpeechPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import platform.Foundation.NSFileManager

/** This module's own graph on iOS. It needs nothing from the platform beyond the system's file manager. */
@DependencyGraph(SpeechScope::class, bindingContainers = [SpeechBindings::class])
internal interface IosSpeechGraph : SpeechGraph {
    @Binds val IosVoice.bindVoice: Voice
    @Binds val IosSpeechCacheLocation.bindLocation: SpeechCacheLocation

    @Provides
    fun provideFileManager(): NSFileManager = NSFileManager.defaultManager
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosSpeechGraphAdapter : SpeechPort by createGraph<IosSpeechGraph>().speech
