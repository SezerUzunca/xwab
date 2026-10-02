package com.xwab.app.composition

import com.xwab.app.navigation.SAVEABLE_ROUTES
import kotlinx.coroutines.flow.emptyFlow
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
 * It runs outside a composition because registering an entry takes nothing from the graph: a
 * feature resolves its ViewModel from `LocalMetroViewModelFactory` only when the entry is drawn.
 */
class AppEntryProviderTest {

    @Test
    fun everySaveableRouteHasAScreen() {
        val entryProvider = appEntryProvider(
            onNavigate = {}, onBack = {}, onReselect = { emptyFlow() },
        )

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

