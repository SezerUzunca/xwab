package com.xwab.app.core.favorites.port

import com.xwab.app.core.sound.port.TrackId
import kotlinx.coroutines.flow.Flow

/** Persists and observes the sounds the user marked as favorites. */
public interface FavoritesPort {
    public val favoriteIds: Flow<Set<TrackId>>

    public suspend fun toggle(trackId: TrackId)
}
