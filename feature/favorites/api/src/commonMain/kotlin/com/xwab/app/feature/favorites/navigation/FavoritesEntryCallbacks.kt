package com.xwab.app.feature.favorites.navigation

import com.xwab.app.core.sound.port.TrackId

/** Actions supplied by the application's composition root. */
class FavoritesEntryCallbacks(
    val onTrackClick: (TrackId) -> Unit,
    val onBrowse: () -> Unit,
)
