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
import kotlinx.serialization.modules.SerializersModule

/**
 * Routes contributed to [FEATURE_SERIALIZERS] preserve their arguments, saved identities and
 * wire names when saved and restored.
 *
 * [SAVEABLE_ROUTES] derives the fixtures from the assembled serializer registrations. The
 * architecture check guards missing registrations; AppEntryProviderTest checks that each
 * registered route also has a screen.
 */
class FeatureSerializersTest {

    private val routeSerializer = PolymorphicSerializer(NavKey::class)
    private val format = Json { serializersModule = FEATURE_SERIALIZERS }

    /** Derived from registrations; AppEntryProviderTest checks the same routes for screens. */
    private val routes: List<NavKey> = SAVEABLE_ROUTES

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

