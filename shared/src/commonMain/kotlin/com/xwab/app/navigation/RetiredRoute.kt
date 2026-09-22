package com.xwab.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * What a route this build no longer has restores as.
 *
 * A route's `@SerialName` is a wire format from the first release on: a saved back stack holds the
 * names of the routes that were on it, and the build reading one back is never the build that wrote
 * it. It may have one feature fewer. Without a fallback the polymorphic lookup finds no serializer
 * for that name and throws, and because the whole stack is decoded as one value, the restore fails
 * entirely — a feature removed between releases crashes the launch after the update, on a screen
 * the listener was nowhere near.
 *
 * [rememberNavigationState] already survives a dropped *tab*: the saved selection falls back to the
 * start tab when it no longer has a stack of its own. That guard only ever ran for a route that
 * still deserialized, though, which a removed one does not. This is the same answer one step
 * earlier — the name resolves to something droppable instead of failing.
 *
 * Nothing navigates here and nothing ever saves it: it exists for the length of one restore, and
 * [dropRetiredRoutes] removes it before the navigation state is built.
 */
internal data object RetiredRoute : NavKey

/**
 * Reads the entry's name and deliberately nothing else.
 *
 * The saved entry still holds whatever the removed route carried — a track id, a category id — and
 * there is no way to know what. A descriptor with no elements is what makes that harmless:
 * `SavedStateDecoder` walks the *descriptor's* elements rather than the saved keys, so an empty one
 * reads nothing and leaves the rest of the entry where it is.
 *
 * Hand-written rather than `@Serializable` for two reasons: `:shared` does not apply the
 * serialization compiler plugin, and the generated object serializer rejects a structure that still
 * has content — which is exactly the structure this one exists to read.
 */
internal object RetiredRouteSerializer : KSerializer<RetiredRoute> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.xwab.app.navigation.RetiredRoute")

    override fun serialize(encoder: Encoder, value: RetiredRoute) {
        encoder.encodeStructure(descriptor) {}
    }

    override fun deserialize(decoder: Decoder): RetiredRoute =
        decoder.decodeStructure(descriptor) { RetiredRoute }
}

/**
 * Drops the entries a restore could not resolve, so a removed feature costs its own screen rather
 * than every screen beneath it.
 *
 * Not optional once [RetiredRoute] exists. `NavDisplay` asks the entry provider for a screen for
 * every key on the stack, and Navigation 3's `entryProvider` throws `Unknown screen` from its
 * default fallback on a key it was never given. Reading the name back instead of failing the
 * restore only moves the crash unless the entry comes off the stack too.
 *
 * Entries are removed where they sit rather than truncating the stack above them: what the listener
 * had open is the entry at the top, and it is still a screen this build can draw. Backing out of it
 * lands one screen earlier than it used to, which is the only trace left of the feature that went
 * away.
 *
 * A tab's root cannot be a [RetiredRoute] — it is a route from [TOP_LEVEL_DESTINATIONS], which is
 * what built the stack in the first place — so a stack cannot empty here. The root goes back anyway
 * if one does: `NavDisplay` rejects an empty entry list outright, with `NavDisplay entries cannot
 * be empty`. An emptied stack would trade the restore crash this function exists to remove for a
 * different one.
 *
 * Not `@Composable`, and kept out of [rememberNavigationState], so the rule it applies is checked
 * without a UI — the same reason [NavigationState] holds no composition of its own.
 */
internal fun dropRetiredRoutes(backStacks: Map<NavKey, MutableList<NavKey>>) {
    backStacks.forEach { (root, backStack) ->
        backStack.removeAll { it is RetiredRoute }
        if (backStack.isEmpty()) backStack.add(root)
    }
}
