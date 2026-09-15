package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.modules.SerializersModule

/**
 * Every route this app can put on a back stack has to be saveable and restorable through
 * [FEATURE_SERIALIZERS].
 *
 * Only the composition root can check this: a feature knows its own route and nothing about the
 * module the app assembles. A route wired into `appEntryProvider` but left out of
 * [FEATURE_SERIALIZERS] costs nothing until a saved back stack comes back, and then
 * `rememberNavBackStack` throws on restore rather than falling back.
 *
 * Add a route here when you add a feature — the list is the contract, not a sample of it.
 */
class FeatureSerializersTest {

    /** Argument values are irrelevant: polymorphic lookup is by type, not by content. */
    private val routes: List<NavKey> = listOf(
        BrowseRoute,
        FavoritesRoute,
        StoriesRoute,
        CategoryRoute("any-category"),
        SoundRoute("any-track"),
    )

    @Test
    fun everyRouteCanBeSaved() {
        routes.forEach { route ->
            assertNotNull(
                FEATURE_SERIALIZERS.serializerFor(route),
                "${route::class.simpleName} has no serializer: its feature's SerializersModule is " +
                    "missing from FEATURE_SERIALIZERS, so saving a back stack holding it fails",
            )
        }
    }

    @Test
    fun everyRouteCanBeRestoredFromTheNameItWasSavedUnder() {
        routes.forEach { route ->
            val serialName = FEATURE_SERIALIZERS.serializerFor(route)?.descriptor?.serialName
            assertNotNull(serialName, "${route::class.simpleName} has no serializer")
            assertNotNull(
                FEATURE_SERIALIZERS.getPolymorphic<NavKey>(NavKey::class, serialName),
                "$serialName cannot be read back, so restoring a back stack holding it throws",
            )
        }
    }

    /**
     * Serial names are a wire format: a saved back stack names its routes with them, so a rename
     * costs every listener a failed restore. Changing this list is changing that format.
     */
    @Test
    fun routeSerialNamesAreTheSavedWireFormat() {
        assertEquals(
            listOf(
                "com.xwab.app.feature.browse.navigation.BrowseRoute",
                "com.xwab.app.feature.favorites.navigation.FavoritesRoute",
                "com.xwab.app.feature.story.navigation.StoriesRoute",
                "com.xwab.app.feature.category.navigation.CategoryRoute",
                "com.xwab.app.feature.sound.navigation.SoundRoute",
            ),
            routes.map { FEATURE_SERIALIZERS.serializerFor(it)?.descriptor?.serialName },
        )
    }
}

private fun SerializersModule.serializerFor(route: NavKey) = getPolymorphic(NavKey::class, route)
