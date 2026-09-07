package com.xwab.app.core.audiodelivery

import com.xwab.app.core.audiodelivery.cache.CachingAudioFileStore
import com.xwab.app.core.audiodelivery.resolution.BackgroundAudioPrefetcher
import com.xwab.app.core.audiodelivery.resolution.LocalFirstSoundContentAdapter
import com.xwab.app.core.network.port.NetworkPort
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.soundsource.port.SoundSourcePort
import okio.FileSystem
import okio.Path

/** Builds the hidden cache stack behind the one public delivery port. */
internal fun createSoundContentAdapter(
    root: Path,
    networkPort: NetworkPort,
    sourcePort: SoundSourcePort,
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
