package com.xwab.app.core.playbacksession.di

import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbacksession.DefaultPlaybackCoordinator
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.SoundPlaybackResolver
import com.xwab.app.core.playbacksession.StoryPlaybackResolver
import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.storymanifest.StoryStreamCatalog
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Binds the playback session adapter.
 *
 * Named for the session rather than "playback" because `core:playback:engine` already contributes
 * the platform [PlaybackController].
 *
 * The two resolvers stay internal to this module: they are constructed inside the provider rather
 * than bound, so no screen can pull one out of the graph and read a URL off it.
 */
@ContributesTo(AppScope::class)
interface PlaybackSessionProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun providePlaybackCoordinator(
        controller: PlaybackController,
        musicCatalog: MusicCatalogRepository,
        audioContent: AudioContentResolver,
        storyCatalog: StoryCatalogRepository,
        storyStreams: StoryStreamCatalog,
    ): PlaybackCoordinator =
        DefaultPlaybackCoordinator(
            controller = controller,
            // One resolver per kind the session can play.
            resolvers = listOf(
                SoundPlaybackResolver(catalog = musicCatalog, content = audioContent),
                StoryPlaybackResolver(catalog = storyCatalog, streams = storyStreams),
            ),
        )
}
