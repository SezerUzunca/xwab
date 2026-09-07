package com.xwab.app.feature.favorites.di

import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.playback.port.PlaybackPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** The three ports this screen reads. Its ViewModel and use case stay internal to the module. */
@SingleIn(AppScope::class)
@Inject
class FavoritesDependencies(
    internal val soundCatalogPort: SoundCatalogPort,
    internal val favoritesPort: FavoritesPort,
    internal val playbackPort: PlaybackPort,
)
