package com.xwab.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.xwab.app.core.audiocontent.AudioFileStore
import com.xwab.app.core.audiocontent.di.audioContentModule
import com.xwab.app.core.audiocontent.di.audioContentPlatformModule
import com.xwab.app.core.domain.port.FavoritesRepository
import com.xwab.app.core.domain.port.MusicCatalogRepository
import com.xwab.app.core.domain.port.PlaybackCoordinator
import com.xwab.app.core.domain.port.AudioContentResolver
import com.xwab.app.core.domain.usecase.CancelSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ObserveCategoryContentUseCase
import com.xwab.app.core.domain.usecase.ObserveHomeContentUseCase
import com.xwab.app.core.domain.usecase.ObservePlayerContentUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackLoopingUseCase
import com.xwab.app.core.domain.usecase.SetPlaybackVolumeUseCase
import com.xwab.app.core.domain.usecase.StartSleepTimerUseCase
import com.xwab.app.core.domain.usecase.ToggleFavoriteUseCase
import com.xwab.app.core.domain.usecase.ToggleMusicPlaybackUseCase
import com.xwab.app.core.media.AudioPlayerState
import com.xwab.app.core.media.PlaybackCommand
import com.xwab.app.core.media.PlaybackController
import com.xwab.app.core.media.SleepTimerState
import com.xwab.app.core.favorites.di.favoritesModule
import com.xwab.app.core.playback.di.playbackCoordinatorModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Catalog, playback and domain wiring is spread over three Koin modules living in three Gradle
 * modules. A binding that no module provides only surfaces when a screen first asks for its use
 * case — at runtime, on a device. These tests turn that into a build failure.
 *
 * Two halves, and both are needed: [theApplicationShipsTheModulesUnderTest] pins the production
 * list in [appModules], and [everyUseCaseResolvesFromTheAssembledGraph] proves that list actually
 * resolves. Either one alone can pass while the app crashes on launch.
 *
 * The feature modules are deliberately not resolved: their ViewModels are `internal` to those
 * modules and are registered as ViewModel definitions, which need a scope this plain container
 * has not got.
 */
class AppModulesTest {
    /** Stands in for the two bindings the platform DI modules contribute. */
    private val platformBindings = module {
        single<DataStore<Preferences>> { FakePreferencesDataStore() }
        single<PlaybackController> { FakePlaybackController() }
        single<AudioFileStore> { FakeAudioFileStore() }
    }

    private val koin = koinApplication {
        modules(audioContentModule, favoritesModule, playbackCoordinatorModule, domainModule, platformBindings)
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
        assertTrue(favoritesModule in shipped, "favoritesModule is missing from appModules()")
        assertTrue(playbackCoordinatorModule in shipped, "playbackCoordinatorModule is missing from appModules()")
        assertTrue(domainModule in shipped, "domainModule is missing from appModules()")
    }

    @Test
    fun everyPortIsBoundToAnImplementation() {
        koin.get<MusicCatalogRepository>()
        koin.get<FavoritesRepository>()
        koin.get<AudioContentResolver>()
        koin.get<PlaybackCoordinator>()
    }

    @Test
    fun everyUseCaseResolvesFromTheAssembledGraph() {
        koin.get<ObserveHomeContentUseCase>()
        koin.get<ObserveCategoryContentUseCase>()
        koin.get<ObservePlayerContentUseCase>()
        koin.get<ToggleFavoriteUseCase>()
        koin.get<ToggleMusicPlaybackUseCase>()
        koin.get<SetPlaybackLoopingUseCase>()
        koin.get<SetPlaybackVolumeUseCase>()
        koin.get<StartSleepTimerUseCase>()
        koin.get<CancelSleepTimerUseCase>()
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
