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
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlin.reflect.KClass

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
                "com.xwab.app.feature.favorites.navigation.FavoritesRoute",
                "com.xwab.app.feature.story.navigation.StoriesRoute",
                "com.xwab.app.feature.category.navigation.CategoryRoute",
                "com.xwab.app.feature.sound.navigation.SoundRoute",
            ),
            routes.map { FEATURE_SERIALIZERS.serializerFor(it)?.descriptor?.serialName },
        )
    }

    /**
     * Closes the gap this list used to leave open.
     *
     * `routes` is hand-written, and so is `FEATURE_SERIALIZERS`. Two hand-written lists of the same
     * thing drift, and the drift that matters is silent: a route added to a feature's serializers
     * but never added here leaves every test above checking a set that no longer describes the app.
     * Reading the registrations back out of the module makes the omission a failing test rather
     * than a quieter list.
     *
     * [RetiredRoute] is subtracted rather than listed: it is the app's own fallback registration,
     * not a route a feature published, and nothing ever navigates to it.
     */
    @Test
    fun theRouteListIsEveryRouteTheModuleActuallyRegisters() {
        val registered = FEATURE_SERIALIZERS.registeredRouteNames() -
            RetiredRouteSerializer.descriptor.serialName

        assertEquals(
            registered.sorted(),
            routes.mapNotNull { FEATURE_SERIALIZERS.serializerFor(it)?.descriptor?.serialName }.sorted(),
            "FEATURE_SERIALIZERS and this test's route list disagree about which routes exist",
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.serializerFor(route: NavKey) = getPolymorphic(NavKey::class, route)

/**
 * The `NavKey` subclasses a module registers, read back out of it.
 *
 * `SerializersModule` has no listing API; `dumpTo` replays the registrations into a collector,
 * which is the only way to ask a module what is in it.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.registeredRouteNames(): Set<String> {
    val names = mutableSetOf<String>()
    dumpTo(
        object : SerializersModuleCollector {
            override fun <T : Any> contextual(
                kClass: KClass<T>,
                provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
            ) = Unit

            override fun <Base : Any, Sub : Base> polymorphic(
                baseClass: KClass<Base>,
                actualClass: KClass<Sub>,
                actualSerializer: KSerializer<Sub>,
            ) {
                if (baseClass == NavKey::class) names += actualSerializer.descriptor.serialName
            }

            override fun <Base : Any> polymorphicDefaultSerializer(
                baseClass: KClass<Base>,
                defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
            ) = Unit

            override fun <Base : Any> polymorphicDefaultDeserializer(
                baseClass: KClass<Base>,
                defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?,
            ) = Unit
        },
    )
    return names
}
