package com.xwab.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.favorites.navigation.FavoritesRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * What a saved back stack does when this build no longer has one of the routes on it.
 *
 * The case only ever happens across releases — one build writes the stack, the next reads it — so
 * nothing about it shows up in a single run of the app. It is checked here because the failure it
 * replaces was the worst shape available: not the removed screen refusing to open, but the restore
 * throwing and the app dying on the launch after the update.
 */
@OptIn(ExperimentalSerializationApi::class)
class RetiredRouteTest {

    /** A plausible name from a build that still had the feature. */
    private val removedRoute = "com.xwab.app.feature.sleeptimer.navigation.SleepTimerRoute"

    private fun stackOf(vararg entries: NavKey): MutableList<NavKey> =
        NavBackStack<NavKey>(*entries)

    @Test
    fun aNameThisBuildNoLongerRegistersResolvesInsteadOfFailingTheRestore() {
        val serializer = FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, removedRoute)

        assertNotNull(
            serializer,
            "an unregistered route name throws on restore, and takes the whole back stack with it",
        )
        assertSame(RetiredRouteSerializer, serializer)
    }

    /**
     * The regression this change could cause, rather than the one it fixes: a fallback that also
     * answered for registered names would turn every route into [RetiredRoute] and empty every
     * back stack the app ever restored.
     */
    @Test
    fun aRouteThisBuildStillHasIsUnaffectedByTheFallback() {
        val name = "com.xwab.app.feature.sound.navigation.SoundRoute"

        assertEquals(
            name,
            FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, name)?.descriptor?.serialName,
        )
    }

    /** The entry goes; what the listener actually had open, and the history below it, stay. */
    @Test
    fun aRetiredEntryIsDroppedWhereItSits() {
        val backStack = stackOf(BrowseRoute, RetiredRoute, SoundRoute("gentle-rain"))

        dropRetiredRoutes(mapOf<NavKey, MutableList<NavKey>>(BrowseRoute to backStack))

        assertEquals(listOf(BrowseRoute, SoundRoute("gentle-rain")), backStack.toList())
    }

    @Test
    fun everyTabIsSwept() {
        val browse = stackOf(BrowseRoute, RetiredRoute)
        val favorites = stackOf(FavoritesRoute, RetiredRoute, SoundRoute("calm-waves"))

        dropRetiredRoutes(
            mapOf<NavKey, MutableList<NavKey>>(
                BrowseRoute to browse,
                FavoritesRoute to favorites,
            ),
        )

        assertEquals(listOf(BrowseRoute), browse.toList())
        assertEquals(listOf(FavoritesRoute, SoundRoute("calm-waves")), favorites.toList())
    }

    /**
     * A tab's root is a route this build still has, so this cannot arise from saved state. It is
     * checked because the alternative to putting the root back is an empty entry list, which
     * `NavDisplay` rejects with `NavDisplay entries cannot be empty` — the restore crash traded for
     * a render one.
     */
    @Test
    fun aStackThatWouldEmptyKeepsItsRoot() {
        val backStack = stackOf(RetiredRoute, RetiredRoute)

        dropRetiredRoutes(mapOf<NavKey, MutableList<NavKey>>(BrowseRoute to backStack))

        assertEquals(listOf(BrowseRoute), backStack.toList())
    }

    @Test
    fun aStackWithNothingToDropIsLeftAlone() {
        val entries = listOf<NavKey>(BrowseRoute, CategoryRoute("rain"), SoundRoute("gentle-rain"))
        val backStack = stackOf(*entries.toTypedArray())

        dropRetiredRoutes(mapOf<NavKey, MutableList<NavKey>>(BrowseRoute to backStack))

        assertEquals(entries, backStack.toList())
    }
}
