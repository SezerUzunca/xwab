package com.xwab.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A back stack written by a build that still had a feature, read by one that no longer does.
 *
 * RetiredRouteTest checks the fallback lookup and the sweep one at a time. This goes through the
 * format `rememberNavBackStack` actually saves in, on the real platform (a `Bundle` on Android), with
 * a removed route that carries arguments: the fallback reads its name and must leave the arguments
 * it has no schema for, without failing the rest of the stack.
 */
class RetiredRouteRestoreTest {
    private val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))

    @Test
    fun aSavedStackWithARemovedRouteAndItsArgumentsRestoresWithoutIt() {
        val olderBuild = SavedStateConfiguration {
            serializersModule = SerializersModule {
                include(FEATURE_SERIALIZERS)
                polymorphic(NavKey::class) { subclass(RemovedRoute::class, RemovedRouteSerializer) }
            }
        }
        val stack = NavBackStack<NavKey>(
            BrowseRoute,
            CategoryRoute("rain"),
            RemovedRoute(minutes = 30, label = "nap"),
            SoundRoute("rain"),
        )
        val saved = encodeToSavedState(serializer, stack, olderBuild)

        // The configuration rememberNavigationState restores with.
        val restored = decodeFromSavedState(
            serializer,
            saved,
            SavedStateConfiguration { serializersModule = FEATURE_SERIALIZERS },
        )
        assertEquals(
            listOf<NavKey>(BrowseRoute, CategoryRoute("rain"), RetiredRoute, SoundRoute("rain")),
            restored.toList(),
        )

        dropRetiredRoutes(mapOf<NavKey, MutableList<NavKey>>(BrowseRoute to restored))
        assertEquals(listOf<NavKey>(BrowseRoute, CategoryRoute("rain"), SoundRoute("rain")), restored.toList())
    }
}

/** A route of a feature this build no longer has, with arguments of more than one type. */
private data class RemovedRoute(val minutes: Int, val label: String) : NavKey

/** Written by hand: `:shared` does not apply the serialization compiler plugin. */
private object RemovedRouteSerializer : KSerializer<RemovedRoute> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.xwab.app.feature.sleeptimer.navigation.SleepTimerRoute") {
            element<Int>("minutes")
            element<String>("label")
        }

    override fun serialize(encoder: Encoder, value: RemovedRoute) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.minutes)
            encodeStringElement(descriptor, 1, value.label)
        }
    }

    override fun deserialize(decoder: Decoder): RemovedRoute =
        error("Only the older build writes this route; this build must read it as RetiredRoute.")
}
