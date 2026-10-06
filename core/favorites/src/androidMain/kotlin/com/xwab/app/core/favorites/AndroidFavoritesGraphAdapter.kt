package com.xwab.app.core.favorites

import android.content.Context
import com.xwab.app.core.favorites.port.FavoritesPort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

/**
 * This module's own graph on Android. It takes the [Context] rather than the files directory, so
 * nothing reads that directory until DataStore first opens the file, off the main thread.
 */
@DependencyGraph(FavoritesScope::class, bindingContainers = [FavoritesBindings::class])
internal interface AndroidFavoritesGraph : FavoritesGraph {
    @Binds val AndroidFavoritesFile.bindFile: FavoritesFile

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidFavoritesGraph
    }
}

/** Hands the module graph's port to the application graph, which builds it once. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
internal class AndroidFavoritesGraphAdapter(
    context: Context,
) : FavoritesPort by createGraphFactory<AndroidFavoritesGraph.Factory>()
    .create(context)
    .favorites
