package com.xwab.app.core.favorites

import android.content.Context
import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.sound.port.TrackId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import okio.Path.Companion.toPath

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidFavoritesAdapter(
    context: Context,
) : FavoritesPort {
    private val delegate = DataStoreFavoritesAdapter(
        createDataStore {
            context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
        },
    )

    override val favoriteIds: Flow<Set<TrackId>> = delegate.favoriteIds

    override suspend fun toggle(trackId: TrackId) = delegate.toggle(trackId)
}
