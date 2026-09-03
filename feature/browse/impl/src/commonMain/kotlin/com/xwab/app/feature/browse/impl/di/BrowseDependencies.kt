package com.xwab.app.feature.browse.impl.di

import com.xwab.app.core.catalog.MusicCatalogRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * What this screen reads, as one value the graph can build and the app can hand to the entry.
 *
 * The ViewModel itself stays `internal`: a compile-time graph can only expose what the module it
 * is generated in can name, so what crosses the boundary is this bag of ports rather than the
 * screen's own class. The entry constructs the ViewModel from it, in this module, where it is
 * visible.
 */
@SingleIn(AppScope::class)
@Inject
class BrowseDependencies(
    internal val musicCatalog: MusicCatalogRepository,
)
