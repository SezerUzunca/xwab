package com.xwab.app.core.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.xwab.app.core.favorites.port.FavoritesPort
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// Required on Kotlin/Native, where IO is an extension rather than a JVM member.
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

/** The lifetime of this module's own graph: one per application, owned by the platform's graph adapter. */
internal object FavoritesScope

/**
 * The adapter and the store behind it: the same on every platform. Only the favorites file differs,
 * and each platform's graph binds its own.
 */
@BindingContainer
internal interface FavoritesBindings {
    @Binds val DataStoreFavoritesAdapter.bindFavorites: FavoritesPort

    companion object {
        /** Where DataStore reads and writes the file; it lives as long as the graph, so the application. */
        @Provides
        @SingleIn(FavoritesScope::class)
        fun provideDataStoreScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /** One store per file: DataStore refuses a second instance over the same path. */
        @Provides
        @SingleIn(FavoritesScope::class)
        fun provideDataStore(file: FavoritesFile, scope: CoroutineScope): DataStore<Preferences> =
            createDataStore(scope, file::path)
    }
}

/**
 * What every platform's favorites graph hands out. Only [FavoritesPort] reaches the application
 * graph, which keeps DataStore inside this module.
 */
internal interface FavoritesGraph {
    val favorites: FavoritesPort
}
