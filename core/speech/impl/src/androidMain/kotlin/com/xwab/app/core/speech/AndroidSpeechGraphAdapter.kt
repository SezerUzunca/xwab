package com.xwab.app.core.speech

import android.content.Context
import com.xwab.app.core.speech.port.SpeechPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/**
 * This module's own graph on Android. It takes the [Context], which the voice needs to reach the
 * device's text-to-speech engine; the cache directory is read from it only when a track is made.
 */
@DependencyGraph(SpeechScope::class, bindingContainers = [SpeechBindings::class])
internal interface AndroidSpeechGraph : SpeechGraph {
    @Binds val AndroidVoice.bindVoice: Voice
    @Binds val AndroidSpeechCacheLocation.bindLocation: SpeechCacheLocation

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidSpeechGraph
    }
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidSpeechGraphAdapter(
    context: Context,
) : SpeechPort by createGraphFactory<AndroidSpeechGraph.Factory>()
    .create(context)
    .speech
