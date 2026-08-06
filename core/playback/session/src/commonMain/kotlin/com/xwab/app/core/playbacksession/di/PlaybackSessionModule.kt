package com.xwab.app.core.playbacksession.di

import com.xwab.app.core.playbacksession.DefaultPlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.SoundPlaybackResolver
import org.koin.dsl.module

/**
 * Binds the playback session adapter.
 *
 * Named for the session rather than "playback" because `core:playback:engine` already owns a
 * `playbackModule` — the one that builds the platform
 * [com.xwab.app.core.playbackengine.api.PlaybackController].
 */
val playbackSessionModule = module {
    single<PlaybackCoordinator> {
        DefaultPlaybackCoordinator(
            controller = get(),
            // One resolver per kind the session can play. Stories join this list the day something
            // can say where a story streams from; nothing outside this module changes for it, and
            // the resolvers stay internal so no screen can resolve one and read a URI out of it.
            resolvers = listOf(SoundPlaybackResolver(catalog = get(), content = get())),
        )
    }
}
