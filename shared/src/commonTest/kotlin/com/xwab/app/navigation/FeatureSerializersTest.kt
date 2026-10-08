package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.modules.SerializersModule

/**
 * Every route this app can put on a back stack has to be saveable and restorable through
 * [FEATURE_SERIALIZERS].
 *
 * Only the composition root can check this: a feature knows its own route and nothing about the
 * module the app assembles. A route with an entry but left out of
 * [FEATURE_SERIALIZERS] costs nothing until a saved back stack comes back, and then
 * `rememberNavBackStack` throws on restore rather than falling back.

 */
class FeatureSerializersTest {

    private val routeSerializer = PolymorphicSerializer(NavKey::class)
    private val format = Json { serializersModule = FEATURE_SERIALIZERS }

    /** Stated once in [SAVEABLE_ROUTES]; AppEntryProviderTest checks the same list for screens. */
    private val routes: List<NavKey> = SAVEABLE_ROUTES

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

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun everyRouteCanBeRestoredFromTheNameItWasSavedUnder() {
        routes.forEach { route ->
            val serialName = FEATURE_SERIALIZERS.serializerFor(route)?.descriptor?.serialName
            assertNotNull(serialName, "${route::class.simpleName} has no serializer")
            assertNotNull(
                FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, serialName),
                "$serialName cannot be read back, so restoring a back stack holding it throws",
            )
        }
    }

    @Test
    fun everyRouteRoundTripsWithItsArgumentsIntact() {
        // Delimiters and non-ASCII characters catch argument loss that a serializer lookup cannot.
        val routesWithArguments = routes + listOf(
            CategoryRoute("rain / yağmur:夜"),
            SoundRoute("sound|\"quoted\"/%25:夜"),
        )
        routesWithArguments.forEach { route ->
            val saved = format.encodeToString(routeSerializer, route)

            assertEquals(route, format.decodeFromString(routeSerializer, saved))
        }
    }

    @Test
    fun aMixedBackStackRoundTripsThroughTheSerializerRememberNavBackStackUses() {
        val stack = NavBackStack<NavKey>(
            BrowseRoute,
            CategoryRoute("rain / yağmur:夜"),
            SoundRoute("rain|night"),
        )
        val serializer = NavBackStackSerializer(routeSerializer)

        val restored = format.decodeFromString(
            serializer,
            format.encodeToString(serializer, stack),
        )

        assertEquals(stack.toList(), restored.toList())
        restored.removeLast()
        assertEquals(3, stack.size, "restoration must create an independent back stack")
    }

    /**
     * Content keys are built from this, so two routes must never share one, whatever their
     * arguments contain. The length prefix is what keeps `"a|1:b"` from imitating two values.
     */
    @Test
    fun everyRouteHasItsOwnSavedIdentity() {
        val tricky = listOf(SoundRoute("a"), SoundRoute("a|1:b"), SoundRoute(""), CategoryRoute("a"))
        val identities = (routes + tricky).map { it.savedIdentity() }

        assertEquals(identities.size, identities.toSet().size, "two routes share an identity: $identities")
        assertEquals("com.xwab.app.feature.sound.navigation.SoundRoute|1:a", SoundRoute("a").savedIdentity())
        assertEquals("com.xwab.app.feature.browse.navigation.BrowseRoute", BrowseRoute.savedIdentity())
    }

    @Test
    fun everySelectedTabRoundTripsAsAPolymorphicNavKey() {
        TOP_LEVEL_DESTINATIONS.forEach { destination ->
            val saved = format.encodeToString(routeSerializer, destination.route)

            assertEquals(destination.route, format.decodeFromString(routeSerializer, saved))
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
                "com.xwab.app.feature.category.navigation.CategoryRoute",
                "com.xwab.app.feature.favorites.navigation.FavoritesRoute",
                "com.xwab.app.feature.sound.navigation.SoundRoute",
                "com.xwab.app.feature.story.navigation.StoriesRoute",
                "com.xwab.app.feature.story.navigation.StoryRoute",
            ),
            routes.map { FEATURE_SERIALIZERS.serializerFor(it)?.descriptor?.serialName }.sortedBy { it },
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.serializerFor(route: NavKey) = getPolymorphic(NavKey::class, route)

