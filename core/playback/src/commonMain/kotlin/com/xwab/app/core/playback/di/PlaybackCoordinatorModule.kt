package com.xwab.app.core.playback.di

import com.xwab.app.core.domain.port.PlaybackCoordinator
import com.xwab.app.core.playback.DefaultPlaybackCoordinator
import org.koin.dsl.module

/**
 * Binds the playback session adapter.
 *
 * Named for the coordinator rather than "playback" because `core:media` already owns a
 * `playbackModule` — the one that builds the platform [com.xwab.app.core.media.PlaybackController].
 */
val playbackCoordinatorModule = module {
    single<PlaybackCoordinator> { DefaultPlaybackCoordinator(get()) }
}
