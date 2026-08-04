package com.xwab.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.audiocontent.resolution.AudioContentResolver
import com.xwab.app.core.audiocontent.cache.AudioFileStore
import com.xwab.app.core.audiocontent.catalog.MusicCatalogRepository
import com.xwab.app.core.audiocontent.di.audioContentModule
import com.xwab.app.core.audiocontent.di.audioContentPlatformModule
import com.xwab.app.core.media.AudioPlayerState
import com.xwab.app.core.media.PlaybackCommand
import com.xwab.app.core.media.PlaybackController
import com.xwab.app.core.media.SleepTimerState
import com.xwab.app.core.playback.PlaybackCoordinator
import com.xwab.app.core.playback.di.playbackCoordinatorModule
import com.xwab.app.core.preferences.FavoritesRepository
import com.xwab.app.core.preferences.di.preferencesModule
import com.xwab.app.core.preferences.di.preferencesPlatformModule
import com.xwab.app.feature.category.navigation.CategoryRoute
import com.xwab.app.feature.home.navigation.HomeRoute
import com.xwab.app.feature.player.navigation.PlayerRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        modules(audioContentModule, preferencesModule, playbackCoordinatorModule, platformBindings)
    }.koin

    @AfterTest
    fun tearDown() = koin.close()

    @Test
    fun theApplicationShipsTheModulesUnderTest() {
        val shipped = appModules()

        assertTrue(audioContentModule in shipped, "audioContentModule is missing from appModules()")
        assertTrue(
            audioContentPlatformModule in shipped,
            "audioContentPlatformModule is missing from appModules()",
        )
        assertTrue(preferencesModule in shipped, "preferencesModule is missing from appModules()")
        assertTrue(
            preferencesPlatformModule in shipped,
            "preferencesPlatformModule is missing from appModules()",
        )
        assertTrue(playbackCoordinatorModule in shipped, "playbackCoordinatorModule is missing from appModules()")
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
        val routes: List<NavKey> = listOf(HomeRoute, CategoryRoute("rain"), PlayerRoute("gentle-rain"))

        routes.forEach { route ->
            assertNotNull(
                featureSerializers.getPolymorphic(NavKey::class, route),
                "no NavKey serializer registered for ${route::class.simpleName}",
            )
        }
    }

    @Test
    fun everyPortIsBoundToAnImplementation() {
        koin.get<MusicCatalogRepository>()
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
