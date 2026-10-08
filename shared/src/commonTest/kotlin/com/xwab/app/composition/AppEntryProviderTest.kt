package com.xwab.app.composition

import com.xwab.app.navigation.SAVEABLE_ROUTES
import com.xwab.app.navigation.appNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every route the app can restore has a screen to draw.
 *
 * The mirror of FeatureSerializersTest. That one checks a route survives being saved; this one
 * checks there is something to show once it comes back. Installing a serializer and contributing an
 * entry installer are separate steps, and a route with only the first restores
 * perfectly and then throws the moment `NavDisplay` asks what to draw — on the launch after an
 * update, for a listener who was simply where they left off.
 *
 * It exercises the real cross-module Metro installer set outside a composition: a feature resolves
 * its ViewModel from `LocalMetroViewModelFactory` only when the entry is drawn.
 */
class AppEntryProviderTest {

    @Test
    fun everySaveableRouteHasAScreen() {
        val entryProvider = appEntryProvider(appEntryGraph(appNavigationState()))

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

