package com.xwab.app.feature.favorites.impl.di

import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** The three ports this screen reads. Its ViewModel and use case stay internal to the module. */
@SingleIn(AppScope::class)
@Inject
class FavoritesDependencies(
    internal val musicCatalog: MusicCatalogRepository,
    internal val favoritesRepository: FavoritesRepository,
    internal val playbackCoordinator: PlaybackCoordinator,
)
