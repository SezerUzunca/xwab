package com.xwab.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation3.runtime.NavKey
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
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.home.navigation.HomeRoute
import com.xwab.app.feature.sounds.navigation.PlayerRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
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
 * The feature modules are deliberately not resolved: their ViewModels and use cases are
 * `internal` to those modules, and the ViewModel definitions need a scope this plain container
 * has not got. Each feature tests its own use case in its own `commonTest` instead.
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
    fun everyRegisteredFeatureReachesTheContainer() {
        val shipped = appModules()

        assertTrue(features.isNotEmpty(), "no feature is registered in `features`")
        features.forEach { feature ->
            assertTrue(
                feature.koinModule in shipped,
                "a registered feature's Koin module is missing from appModules()",
            )
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
            listOf(HomeRoute, CategoryRoute("rain"), PlayerRoute("gentle-rain"), StoriesRoute)

        routes.forEach { route ->
            assertNotNull(
                featureSerializers.getPolymorphic(NavKey::class, route),
                "no NavKey serializer registered for ${route::class.simpleName}",
            )
        }
    }

    /**
     * `AppShell` builds the navigation bar from this list and starts on its first entry, so an
     * empty one is a blank app and two features claiming the same route are two tabs sharing a
     * single back stack — each tab's stack is keyed by its route.
     */
    @Test
    fun theNavigationBarIsBuiltFromTheFeaturesWithoutADuplicateRoute() {
        assertTrue(topLevelDestinations.isNotEmpty(), "no feature declares a TopLevelDestination")
        assertEquals(
            topLevelDestinations.size,
            topLevelDestinations.map { it.route }.toSet().size,
            "two features declare the same top-level route",
        )
    }

    /**
     * Derived from the bar rather than listed by hand, so a tab added later is covered the day it
     * appears. A tab's root without a serializer is a crash on the second launch, not the first.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun everyTabsRootRouteHasASerializer() {
        topLevelDestinations.forEach { destination ->
            assertNotNull(
                featureSerializers.getPolymorphic(NavKey::class, destination.route),
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
