package com.xwab.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.xwab.app.composition.BrowseTabChild
import com.xwab.app.composition.DefaultAppComponent
import com.xwab.app.composition.FavoritesTabChild
import com.xwab.app.composition.StoriesTabChild
import com.xwab.app.core.audiodelivery.cache.AudioFileStore
import com.xwab.app.core.audiodelivery.di.audioDeliveryModule
import com.xwab.app.core.audiodelivery.di.audioDeliveryPlatformModule
import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.catalog.CategoryId
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
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
import com.xwab.app.feature.category.api.navigation.CategoryConfig
import com.xwab.app.feature.category.impl.di.categoryModule
import com.xwab.app.feature.favorites.impl.di.favoritesFeatureModule
import com.xwab.app.feature.sounds.api.navigation.PlayerConfig
import com.xwab.app.feature.sounds.impl.di.soundsModule
import com.xwab.app.feature.story.impl.di.storyModule
import com.xwab.app.navigation.AppTab
import com.xwab.app.navigation.BrowseTabConfig
import com.xwab.app.navigation.FavoritesTabConfig
import com.xwab.app.navigation.StoriesTabConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
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
@OptIn(ExperimentalCoroutinesApi::class)
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
            favoritesFeatureModule,
            categoryModule,
            soundsModule,
            storyModule,
            platformBindings,
        )
    }.koin

    // Components built below call `componentScope()`, which resolves `Dispatchers.Main` eagerly.
    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() {
        koin.close()
        Dispatchers.resetMain()
    }

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
        val expected = listOf(favoritesFeatureModule, categoryModule, soundsModule, storyModule)

        assertEquals(expected, featureModules, "the app's explicit feature module list changed")
        expected.forEach { featureModule ->
            assertTrue(featureModule in shipped, "a feature module is missing from appModules()")
        }
    }

    /**
     * Every case in every tab's `Config` round-trips through the format the real back stack is
     * persisted in. A case that doesn't only fails when the back stack is restored after process
     * death — a crash on the second launch, not the first.
     */
    @Test
    fun everyTabConfigRoundTripsThroughSerialization() {
        val browseConfigs: List<BrowseTabConfig> = listOf(
            BrowseTabConfig.Root,
            BrowseTabConfig.Category(CategoryConfig("rain")),
            BrowseTabConfig.Player(PlayerConfig("gentle-rain")),
        )
        val favoritesConfigs: List<FavoritesTabConfig> = listOf(
            FavoritesTabConfig.Root,
            FavoritesTabConfig.Player(PlayerConfig("gentle-rain")),
        )
        val storiesConfigs: List<StoriesTabConfig> = listOf(StoriesTabConfig.Root)

        browseConfigs.forEach { config ->
            val json = Json.encodeToString(BrowseTabConfig.serializer(), config)
            assertEquals(config, Json.decodeFromString(BrowseTabConfig.serializer(), json))
        }
        favoritesConfigs.forEach { config ->
            val json = Json.encodeToString(FavoritesTabConfig.serializer(), config)
            assertEquals(config, Json.decodeFromString(FavoritesTabConfig.serializer(), json))
        }
        storiesConfigs.forEach { config ->
            val json = Json.encodeToString(StoriesTabConfig.serializer(), config)
            assertEquals(config, Json.decodeFromString(StoriesTabConfig.serializer(), json))
        }
    }

    /**
     * A config no feature claims compiles fine and throws the first time something navigates to
     * it — the `when` in `DefaultAppComponent`'s child factories is exhaustive, so a missing case
     * is now a compile error instead, but a factory that resolves the *wrong* component, or fails
     * to actually wire a push, would not be. This drives real navigation through the real
     * component tree and checks what each tab shows before and after.
     */
    @Test
    fun everyTabsRootAndPushedConfigsResolveToTheRightComponent() {
        val root = DefaultAppComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            koin = koin,
        )

        assertEquals(AppTab.BROWSE, root.selectedTab.value, "Browse must remain the start tab")

        val browseRoot = assertIs<BrowseTabChild.Root>(root.browseStack.value.active.instance)
        browseRoot.component.onCategoryClick(CategoryId("rain"))
        val browseCategory = assertIs<BrowseTabChild.Category>(root.browseStack.value.active.instance)
        browseCategory.component.onMusicClick(TrackId("gentle-rain"))
        assertIs<BrowseTabChild.Player>(root.browseStack.value.active.instance)

        val favoritesRoot = assertIs<FavoritesTabChild.Root>(root.favoritesStack.value.active.instance)
        favoritesRoot.component.onMusicClick(TrackId("gentle-rain"))
        assertIs<FavoritesTabChild.Player>(root.favoritesStack.value.active.instance)

        assertIs<StoriesTabChild.Root>(root.storiesStack.value.active.instance)
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
