package com.xwab.app.composition

import com.xwab.app.content.ContentCacheMaintenance
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.browse.di.BrowseDependencies
import com.xwab.app.feature.category.di.CategoryDependencies
import com.xwab.app.feature.favorites.di.FavoritesDependencies
import com.xwab.app.feature.nowplaying.di.NowPlayingDependencies
import com.xwab.app.feature.sound.di.SoundDependencies
import com.xwab.app.feature.story.di.StoriesDependencies
import com.xwab.app.navigation.SAVEABLE_ROUTES
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every route the app can restore has a screen to draw.
 *
 * The mirror of FeatureSerializersTest. That one checks a route survives being saved; this one
 * checks there is something to show once it comes back. Registering a serializer and registering an
 * entry are two separate lines in two separate files, and a route with only the first restores
 * perfectly and then throws the moment `NavDisplay` asks what to draw — on the launch after an
 * update, for a listener who was simply where they left off.
 *
 * [NoDependencies] is what lets this run outside a composition, and it carries an assertion of its
 * own: registering an entry must not build a feature's dependency bag. `appEntryProvider` states
 * that providers are invoked inside ViewModel initializers, and a graph that throws on every
 * accessor is what keeps that true rather than merely written down.
 */
class AppEntryProviderTest {

    @Test
    fun everySaveableRouteHasAScreen() {
        val entryProvider = appEntryProvider(NoDependencies, onNavigate = {}, onBack = {})

        // Resolving is the assertion: Navigation 3's `entryProvider` throws `Unknown screen` from
        // its fallback for a key it was never given. The content keys are kept because they carry a
        // second invariant — two routes sharing one would be a single entry to `NavDisplay`.
        val contentKeys = SAVEABLE_ROUTES.map { entryProvider(it).contentKey }

        assertEquals(
            SAVEABLE_ROUTES.size,
            contentKeys.distinct().size,
            "two saveable routes resolve to the same content key: $contentKeys",
        )
    }
}

private fun unused(): Nothing =
    error("Registering an entry must not build a feature's dependencies.")

private object NoDependencies : AppGraph {
    override val browseDependencies: () -> BrowseDependencies = { unused() }
    override val favoritesDependencies: () -> FavoritesDependencies = { unused() }
    override val categoryDependencies: () -> CategoryDependencies = { unused() }
    override val soundDependencies: () -> SoundDependencies = { unused() }
    override val storiesDependencies: () -> StoriesDependencies = { unused() }
    override val nowPlayingDependencies: () -> NowPlayingDependencies = { unused() }
    override val contentCacheMaintenance: () -> ContentCacheMaintenance = { unused() }
}
