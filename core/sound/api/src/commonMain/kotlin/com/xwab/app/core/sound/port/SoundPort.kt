package com.xwab.app.core.sound.port

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Read access to sound metadata. Physical sources are deliberately a separate capability. */
interface SoundPort {
    fun observeCategories(): Flow<List<Category>>
    fun observeAllTracks(): Flow<List<Track>>
    fun observeCategory(categoryId: CategoryId): Flow<Category?>
    fun observeTracksForCategory(categoryId: CategoryId): Flow<List<Track>>
    fun observeTrack(trackId: TrackId): Flow<Track?>

    /** Confirmed local availability; observation never downloads or starts playback. */
    fun observeOfflineReady(trackId: TrackId): Flow<Boolean> = flowOf(false)
}
