package com.xwab.app.feature.sounds.impl.di

import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The three ports this screen reads.
 *
 * Looping, volume and the sleep timer reach the coordinator straight from the ViewModel — they
 * carry no decision, so there is nothing for a use case to own and nothing more to bind here.
 */
@SingleIn(AppScope::class)
@Inject
class PlayerDependencies(
    internal val musicCatalog: MusicCatalogRepository,
    internal val favoritesRepository: FavoritesRepository,
    internal val playbackCoordinator: PlaybackCoordinator,
)
