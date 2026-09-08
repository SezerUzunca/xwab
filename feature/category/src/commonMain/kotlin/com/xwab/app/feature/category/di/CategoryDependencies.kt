package com.xwab.app.feature.category.di

import com.xwab.app.core.sound.port.SoundPort
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.session.port.PlaybackPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** The three ports this screen reads. Its ViewModel and use case stay internal to the module. */
@SingleIn(AppScope::class)
@Inject
class CategoryDependencies(
    internal val soundPort: SoundPort,
    internal val favoritesPort: FavoritesPort,
    internal val playbackPort: PlaybackPort,
)
