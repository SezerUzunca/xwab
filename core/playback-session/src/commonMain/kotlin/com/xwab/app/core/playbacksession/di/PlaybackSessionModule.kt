package com.xwab.app.core.playbacksession.di

import com.xwab.app.core.playbacksession.DefaultPlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import org.koin.dsl.module

/**
 * Binds the playback session adapter.
 *
 * Named for the session rather than "playback" because `core:playback-engine` already owns a
 * `playbackModule` — the one that builds the platform
 * [com.xwab.app.core.playbackengine.api.PlaybackController].
 */
val playbackSessionModule = module {
    single<PlaybackCoordinator> { DefaultPlaybackCoordinator(get(), get(), get()) }
}
