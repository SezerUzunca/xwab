package com.xwab.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation3.runtime.NavKey
import com.xwab.app.composition.appEntryProvider
import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.audiodelivery.di.audioDeliveryModule
import com.xwab.app.core.audiodelivery.di.audioDeliveryPlatformModule
import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalogmanifest.di.catalogManifestModule
import com.xwab.app.core.favorites.FavoritesRepository
import com.xwab.app.core.favorites.di.favoritesModule
import com.xwab.app.core.favorites.di.favoritesPlatformModule
import com.xwab.app.core.network.NetworkClient
import com.xwab.app.core.network.di.networkModule
import com.xwab.app.core.playbackengine.api.AudioPlayerState
import com.xwab.app.core.playbackengine.api.PlaybackCommand
import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbackengine.api.SleepTimerState
import com.xwab.app.core.playbackengine.di.playbackModule
import com.xwab.app.core.playbacksession.PlaybackCoordinator
import com.xwab.app.core.playbacksession.di.playbackSessionModule
import com.xwab.app.core.story.StoryCatalogRepository
import com.xwab.app.core.storymanifest.di.storyManifestModule
import com.xwab.app.feature.browse.api.navigation.BrowseRoute
import com.xwab.app.feature.browse.impl.di.browseModule
import com.xwab.app.feature.category.api.navigation.CategoryRoute
import com.xwab.app.feature.category.impl.di.categoryModule
import com.xwab.app.feature.favorites.api.navigation.FavoritesRoute
import com.xwab.app.feature.favorites.impl.di.favoritesFeatureModule
import com.xwab.app.feature.sounds.api.navigation.PlayerRoute
import com.xwab.app.feature.sounds.impl.di.soundsModule
import com.xwab.app.feature.story.api.navigation.StoriesRoute
import com.xwab.app.feature.story.impl.di.storyModule
import com.xwab.app.navigation.FEATURE_SERIALIZERS
import com.xwab.app.navigation.NavigationState
import com.xwab.app.navigation.Navigator
import com.xwab.app.navigation.TOP_LEVEL_DESTINATIONS
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Catalog and playback wiring is spread over several Koin modules living in
 * several Gradle modules. A binding that no module provides only surfaces when a screen first
 * asks for it — at runtime, on a device. These tests turn that into a build failure.
 *
 * Runtime feature definitions are verified separately as a complete graph; these tests exercise
 * the app composition contract and the real core adapter bindings.
 */
class AppModulesTest {
    /** Stands in for the bindings the platform DI modules contribute. */
    private val platformBindings = module {
        single<DataStore<Preferences>> { FakePreferencesDataStore() }
        single<PlaybackController> { FakePlaybackController() }
        single<AudioFileStore> { FakeAudioFileStore() }
    }

    private val koin = koinApplication {
        modules(
            networkModule,
            catalogManifestModule,
            audioDeliveryModule,
            storyManifestModule,
            favoritesModule,
            playbackSessionModule,
            platformBindings,
        )
    }.koin

    @AfterTest
    fun tearDown() = koin.close()

    @Test
    fun theApplicationShipsTheModulesUnderTest() {
        val shipped = appModules()

        assertTrue(networkModule in shipped, "networkModule is missing from appModules()")
        assertTrue(catalogManifestModule in shipped, "catalogManifestModule is missing from appModules()")
        assertTrue(audioDeliveryModule in shipped, "audioDeliveryModule is missing from appModules()")
        assertTrue(
            audioDeliveryPlatformModule in shipped,
            "audioDeliveryPlatformModule is missing from appModules()",
        )
        assertTrue(storyManifestModule in shipped, "storyManifestModule is missing from appModules()")
        assertTrue(favoritesModule in shipped, "favoritesModule is missing from appModules()")
        assertTrue(
            favoritesPlatformModule in shipped,
            "favoritesPlatformModule is missing from appModules()",
        )
        assertTrue(playbackSessionModule in shipped, "playbackSessionModule is missing from appModules()")
        // The container below stands a fake PlaybackController in for this module, so nothing else
        // here would notice if the real platform binding stopped being shipped.
        assertTrue(playbackModule in shipped, "playbackModule is missing from appModules()")
    }

    @Test
    fun everyFeatureImplementationReachesTheContainer() {
        val shipped = appModules()
        val expected = listOf(browseModule, favoritesFeatureModule, categoryModule, soundsModule, storyModule)

        assertEquals(expected, featureModules, "the app's explicit feature module list changed")
        expected.forEach { featureModule ->
            assertTrue(featureModule in shipped, "a feature module is missing from appModules()")
        }
    }

    /**
     * A route whose serializer is missing compiles fine and only fails when the back stack is
     * restored after process death — a crash on the second launch, not the first.
     *
     * `getPolymorphic` is the only way to ask a `SerializersModule` what it holds, and it carries
     * an opt-in; the alternative is not asking, which is what this test exists to stop.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun everyFeatureContributesTheSerializersForItsOwnRoutes() {
        val routes: List<NavKey> =
            listOf(BrowseRoute, FavoritesRoute, CategoryRoute("rain"), PlayerRoute("gentle-rain"), StoriesRoute)

        routes.forEach { route ->
            assertNotNull(
                FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, route),
                "no NavKey serializer registered for ${route::class.simpleName}",
            )
        }
    }

    /**
     * Migrated routes keep their old wire names so an installed app can restore its back stack.
     *
     * `rememberNavBackStack` writes each entry's serial name as a polymorphic type discriminator,
     * so these strings are a storage format, not an implementation detail. Each one below is the
     * name that reached `main` — not an intermediate package from a branch, which no installed app
     * ever wrote.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun migratedRoutesRestoreTheirLegacyNames() {
        val legacyRouteNames = listOf(
            "com.xwab.app.feature.home.navigation.HomeRoute",
            "com.xwab.app.feature.category.navigation.CategoryRoute",
            "com.xwab.app.feature.sounds.navigation.PlayerRoute",
            "com.xwab.app.feature.story.navigation.StoriesRoute",
        )

        legacyRouteNames.forEach { serialName ->
            assertNotNull(
                FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, serialName),
                "$serialName must remain restorable after the API/impl migration",
            )
        }
    }

    /**
     * A route no feature claims compiles fine and throws the first time something navigates to it.
     *
     * Registration moved out of the Koin container and into each feature's `entry<Route> { }`, so
     * the container no longer even indirectly vouches for it. Nothing here is composed — building
     * the provider and asking it for a key resolves the entry without running its content.
     */
    @Test
    fun everyRouteResolvesToAnEntry() {
        val routes: List<NavKey> =
            listOf(BrowseRoute, FavoritesRoute, CategoryRoute("rain"), PlayerRoute("gentle-rain"), StoriesRoute)
        // Every route gets a stack of its own: this navigator is never driven, it is only what the
        // features are handed while they register.
        val navigator = Navigator(
            NavigationState(
                startRoute = BrowseRoute,
                backStacks = routes.associateWith { mutableListOf(it) },
            ),
        )

        val provider = appEntryProvider(navigator::navigate, navigator::goBack)

        routes.forEach { route ->
            assertNotNull(provider(route), "no entry registered for ${route::class.simpleName}")
        }
    }

    /**
     * The app shell builds the navigation bar from this list and starts on its first entry, so an
     * empty one is a blank app and two features claiming the same route are two tabs sharing a
     * single back stack — each tab's stack is keyed by its route.
     */
    @Test
    fun theNavigationBarIsBuiltFromTheFeaturesWithoutADuplicateRoute() {
        assertTrue(TOP_LEVEL_DESTINATIONS.isNotEmpty(), "the app declares no top-level destination")
        assertEquals(
            TOP_LEVEL_DESTINATIONS.size,
            TOP_LEVEL_DESTINATIONS.map { it.route }.toSet().size,
            "the app declares the same top-level route twice",
        )
        assertEquals(BrowseRoute, TOP_LEVEL_DESTINATIONS.first().route, "Browse must remain the start route")
    }

    /**
     * Derived from the bar rather than listed by hand, so a tab added later is covered the day it
     * appears. A tab's root without a serializer is a crash on the second launch, not the first.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun everyTabsRootRouteHasASerializer() {
        TOP_LEVEL_DESTINATIONS.forEach { destination ->
            assertNotNull(
                FEATURE_SERIALIZERS.getPolymorphic(NavKey::class, destination.route),
                "no NavKey serializer registered for the tab rooted at ${destination.route}",
            )
        }
    }

    @Test
    fun everyPortIsBoundToAnImplementation() {
        koin.get<NetworkClient>()
        koin.get<MusicCatalogRepository>()
        koin.get<StoryCatalogRepository>()
        koin.get<FavoritesRepository>()
        koin.get<AudioContentResolver>()
        koin.get<PlaybackCoordinator>()
    }

    private class FakePreferencesDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(emptyPreferences())
    }

    private class FakePlaybackController : PlaybackController {
        override val state: StateFlow<AudioPlayerState> = MutableStateFlow(AudioPlayerState())
        override val sleepTimerState: StateFlow<SleepTimerState> = MutableStateFlow(SleepTimerState())
        override fun submit(command: PlaybackCommand) = Unit
        override fun release() = Unit
    }

    private class FakeAudioFileStore : AudioFileStore {
        override suspend fun find(cacheFileName: String): String? = null
        override suspend fun download(cacheFileName: String, remoteHttpsUrl: String) = Unit
    }
}
