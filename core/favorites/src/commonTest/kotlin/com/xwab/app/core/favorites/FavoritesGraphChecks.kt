package com.xwab.app.core.favorites

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import kotlin.test.assertSame

/**
 * A platform's production graph, with its favorites file replaced by [UnresolvableFile]. The app
 * graph may build this graph on the main thread, so building it must touch no file; and it must
 * hand out one adapter, so one store per file.
 */
internal fun checkTheModuleGraphSharesOneAdapterAndLeavesTheFileForItsFirstAccess(graph: FavoritesGraph) {
    assertSame(graph.favorites, graph.favorites)
}

/** Replaces the platform's favorites file with one that fails the test if anything resolves it. */
@BindingContainer
internal object UnresolvableFile {
    @Provides
    fun file(): FavoritesFile = FavoritesFile { error("The DataStore file was resolved while the graph was built.") }
}
