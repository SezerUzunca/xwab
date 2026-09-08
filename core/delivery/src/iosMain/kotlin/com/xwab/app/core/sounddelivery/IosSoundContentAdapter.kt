@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.sounddelivery

import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.sound.port.SoundPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosSoundContentAdapter(
    networkPort: NetworkPort,
    sourcePort: SoundPort,
) : SoundContentPort by createSoundContentAdapter(
    root = iosAudioCachePath(),
    networkPort = networkPort,
    sourcePort = sourcePort,
)

private fun iosAudioCachePath() = (
    requireNotNull(
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path,
    ) + "/audio-content"
).toPath()
