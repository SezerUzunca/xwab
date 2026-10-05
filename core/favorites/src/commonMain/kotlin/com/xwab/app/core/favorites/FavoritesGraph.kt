package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwab.app.core.favorites.port.FavoritesPort
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object FavoritesScope

/**
 * This module's own graph, which keeps DataStore inside the module: only [FavoritesPort] reaches
 * the application graph. A binding container contributed to `AppScope` would have to be public to
 * reach that graph, and core declarations outside `.port` stay internal.
 */
@DependencyGraph(FavoritesScope::class)
internal interface FavoritesGraph {
    val favorites: FavoritesPort

    @Binds val DataStoreFavoritesAdapter.bindFavorites: FavoritesPort

    /** One store per file: DataStore refuses a second instance over the same path. */
    @Provides
    @SingleIn(FavoritesScope::class)
    fun provideDataStore(file: FavoritesFile): DataStore<Preferences> = createDataStore(file::path)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides file: FavoritesFile): FavoritesGraph
    }
}
