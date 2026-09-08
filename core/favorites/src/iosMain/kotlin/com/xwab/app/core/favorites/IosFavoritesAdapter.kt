@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.xwab.app.core.favorites

import com.xwab.app.core.favorites.port.FavoritesPort
import com.xwab.app.core.sound.port.TrackId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class IosFavoritesAdapter : FavoritesPort {
    private val delegate = DataStoreFavoritesAdapter(
        createDataStore {
            val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            (requireNotNull(documentDirectory).path + "/$DATA_STORE_FILE_NAME").toPath()
        },
    )

    override val favoriteIds: Flow<Set<TrackId>> = delegate.favoriteIds

    override suspend fun toggle(trackId: TrackId) = delegate.toggle(trackId)
}
