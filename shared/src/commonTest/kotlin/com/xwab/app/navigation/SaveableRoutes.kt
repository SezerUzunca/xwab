package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlin.reflect.KClass

/**
 * One instance of every route this app can put on a saved back stack, read out of
 * [FEATURE_SERIALIZERS] rather than written down.
 *
 * Two tests need it and they check opposite halves of the same contract: FeatureSerializersTest
 * that each route survives being saved and read back, AppEntryProviderTest that each one has a
 * screen to draw when it comes back. Features contribute their routes through Metro and
 * checkArchitecture rule 23 makes every declared route part of that contribution, so reading the
 * registrations back covers a new feature's routes without anyone listing them here.
 *
 * Each instance is decoded from its serializer with a placeholder for every argument. Routes carry
 * plain string ids by design — a route is a wire format — and an argument of any other kind fails
 * here by name rather than being guessed. Both lookups are by type, not by content.
 */
internal val SAVEABLE_ROUTES: List<NavKey> = FEATURE_SERIALIZERS.routeSerializers()
    .filterNot { it.descriptor.serialName == RetiredRouteSerializer.descriptor.serialName }
    .map { serializer -> Json.decodeFromJsonElement(serializer, placeholderArguments(serializer)) }

@OptIn(ExperimentalSerializationApi::class)
private fun placeholderArguments(serializer: KSerializer<out NavKey>) = buildJsonObject {
    val descriptor = serializer.descriptor
    repeat(descriptor.elementsCount) { index ->
        val name = descriptor.getElementName(index)
        check(descriptor.getElementDescriptor(index).kind == PrimitiveKind.STRING) {
            "${descriptor.serialName}.$name is not a String; give SAVEABLE_ROUTES a value for it."
        }
        put(name, JsonPrimitive("any-$name"))
    }
}

/**
 * The `NavKey` subclasses a module registers, read back out of it.
 *
 * `SerializersModule` has no listing API; `dumpTo` replays the registrations into a collector,
 * which is the only way to ask a module what is in it. [RetiredRoute] is among them: it is the
 * app's own fallback registration, which [SAVEABLE_ROUTES] leaves out.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun SerializersModule.routeSerializers(): List<KSerializer<out NavKey>> {
    val serializers = mutableListOf<KSerializer<out NavKey>>()
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
                @Suppress("UNCHECKED_CAST")
                if (baseClass == NavKey::class) serializers += actualSerializer as KSerializer<out NavKey>
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
    return serializers
}
