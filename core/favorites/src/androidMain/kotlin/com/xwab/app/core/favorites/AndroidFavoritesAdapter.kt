package com.xwab.app.core.favorites

import android.content.Context
import com.xwab.app.core.favorites.port.FavoritesPort
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

    override fun observe(namespace: String): Flow<Set<String>> = delegate.observe(namespace)

    override suspend fun toggle(namespace: String, itemId: String) = delegate.toggle(namespace, itemId)
}
