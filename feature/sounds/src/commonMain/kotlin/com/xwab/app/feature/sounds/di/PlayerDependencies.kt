package com.xwab.app.feature.sounds.di

import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.playback.port.PlaybackPort
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
    internal val soundCatalogPort: SoundCatalogPort,
    internal val favoritesPort: FavoritesPort,
    internal val playbackPort: PlaybackPort,
)
