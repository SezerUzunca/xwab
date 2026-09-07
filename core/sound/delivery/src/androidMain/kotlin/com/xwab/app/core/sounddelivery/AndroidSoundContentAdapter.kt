package com.xwab.app.core.sounddelivery

import android.content.Context
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.soundsource.port.SoundSourcePort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidSoundContentAdapter(
    context: Context,
    networkPort: NetworkPort,
    sourcePort: SoundSourcePort,
) : SoundContentPort by createSoundContentAdapter(
    root = context.cacheDir.resolve("audio-content").absolutePath.toPath(),
    networkPort = networkPort,
    sourcePort = sourcePort,
)
