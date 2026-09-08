package com.xwab.app.core.sounddelivery

import com.xwab.app.core.sounddelivery.cache.CachingAudioFileStore
import com.xwab.app.core.sounddelivery.resolution.BackgroundAudioPrefetcher
import com.xwab.app.core.sounddelivery.resolution.LocalFirstSoundContentAdapter
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.sound.port.SoundPort
import okio.FileSystem
import okio.Path

/** Builds the hidden cache stack behind the one public delivery port. */
internal fun createSoundContentAdapter(
    root: Path,
    networkPort: NetworkPort,
    sourcePort: SoundPort,
): SoundContentPort {
    val fileStore = CachingAudioFileStore(
        fileSystem = FileSystem.SYSTEM,
        root = root,
        networkPort = networkPort,
        sourcePort = sourcePort,
    )
    return LocalFirstSoundContentAdapter(
        fileStore = fileStore,
        prefetcher = BackgroundAudioPrefetcher(fileStore),
        sourcePort = sourcePort,
    )
}
