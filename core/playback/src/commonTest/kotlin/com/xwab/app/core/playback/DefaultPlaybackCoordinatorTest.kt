package com.xwab.app.core.playback

import com.xwab.app.core.domain.port.AudioContentResolver
import com.xwab.app.core.domain.port.PlaybackSummary
import com.xwab.app.core.domain.port.ResolvedAudioContent
import com.xwab.app.core.media.AudioPlayerState
import com.xwab.app.core.media.AudioSource
import com.xwab.app.core.media.LoopMode
import com.xwab.app.core.media.PlaybackCommand
import com.xwab.app.core.media.PlaybackController
import com.xwab.app.core.media.PlaybackPhase
import com.xwab.app.core.media.PlaybackRequest
import com.xwab.app.core.media.SleepTimerState
import com.xwab.app.core.model.Music
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultPlaybackCoordinatorTest {
    private val testContentResolver = AudioContentResolver { musicId ->
        ResolvedAudioContent("test://$musicId", isLocal = true)
    }

    @Test
    fun togglePlaybackReloadsFailedRequestedSourceWithAutoplay() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Failed,
                isLooping = true,
            )
        }
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.toggleBlocking(gentleRain())

        assertEquals(true, player.lastLoadRequest?.autoplay)
        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    @Test
    fun togglePlaybackPausesOrPlaysTheActiveSourceWithoutReloading() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = true,
                isPlaying = true,
            )
        }
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.toggleBlocking(gentleRain())

        assertEquals(1, player.pauseCalls)
        assertEquals(null, player.lastLoadRequest)
    }

    @Test
    fun togglePlaybackPausesDesiredPlaybackWhileAutoplayIsBuffering() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Buffering,
                playRequested = true,
                isPlaying = false,
            )
        }
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.toggleBlocking(gentleRain())

        assertEquals(1, player.pauseCalls)
        assertEquals(0, player.playCalls)
        assertEquals(null, player.lastLoadRequest)
    }

    @Test
    fun togglePlaybackPlaysActiveSourceWhenPlaybackIsNotDesired() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = false,
                isPlaying = false,
            )
        }
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.toggleBlocking(gentleRain())

        assertEquals(1, player.playCalls)
        assertEquals(0, player.pauseCalls)
        assertEquals(null, player.lastLoadRequest)
    }

    @Test
    fun theFirstSoundLoopsBecauseThatIsTheProductDefault() {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.toggleBlocking(gentleRain())

        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    @Test
    fun settingsChosenBeforeTheFirstLoadBeatTheProductDefault() {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.setLooping(false)
        coordinator.setVolume(0.42f)
        coordinator.toggleBlocking(gentleRain())

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
        assertEquals(false, player.lastLooping)
        assertEquals(0.42f, player.lastVolume)
    }

    @Test
    fun playbackSettingsAreRetainedWhenAnotherSoundIsLoaded() {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)
        coordinator.toggleBlocking(gentleRain())
        player.attachRequestedSource()

        coordinator.setLooping(false)
        coordinator.setVolume(0.42f)
        coordinator.toggleBlocking(calmWaves())

        assertEquals("calm-waves", player.lastLoadRequest?.source?.id)
        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
    }

    @Test
    fun settingsChangedOutsideTheAppSurviveALostServiceConnection() {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)
        coordinator.toggleBlocking(gentleRain())
        player.attachRequestedSource()

        // A notification or Bluetooth control changes the settings behind the app's back;
        // the reducer adopts them, so they reach the coordinator through the published state.
        player.mutableState.update { it.copy(isLooping = false, volume = 0.3f) }
        // The service connection then drops, which clears only the *attached* source.
        player.mutableState.update { it.copy(source = null) }

        coordinator.toggleBlocking(calmWaves())

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.3f, player.lastLoadRequest?.volume)
    }

    @Test
    fun anOlderSlowResolutionCannotReplaceTheUsersNewerSelection() = runBlocking {
        val rainStarted = CompletableDeferred<Unit>()
        val wavesStarted = CompletableDeferred<Unit>()
        val rainResult = CompletableDeferred<ResolvedAudioContent>()
        val wavesResult = CompletableDeferred<ResolvedAudioContent>()
        val resolver = AudioContentResolver { musicId ->
            when (musicId) {
                "gentle-rain" -> {
                    rainStarted.complete(Unit)
                    rainResult.await()
                }
                "calm-waves" -> {
                    wavesStarted.complete(Unit)
                    wavesResult.await()
                }
                else -> null
            }
        }
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, resolver)

        val olderRequest = launch { coordinator.togglePlayback(gentleRain()) }
        rainStarted.await()
        val newerRequest = launch { coordinator.togglePlayback(calmWaves()) }
        wavesStarted.await()

        wavesResult.complete(ResolvedAudioContent("https://example.test/waves.mp3", isLocal = false))
        newerRequest.join()
        rainResult.complete(ResolvedAudioContent("https://example.test/rain.mp3", isLocal = false))
        olderRequest.join()

        assertEquals("calm-waves", player.lastLoadRequest?.source?.id)
    }

    @Test
    fun sleepTimerCommandsAreForwardedToThePlayer() {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        coordinator.startSleepTimer(15 * 60_000L)
        coordinator.cancelSleepTimer()

        assertEquals(15 * 60_000L, player.lastSleepTimerDurationMs)
        assertEquals(1, player.cancelSleepTimerCalls)
    }

    @Test
    fun nonFiniteVolumeIsRejectedWithoutPoisoningTheNextLoad() {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)
        coordinator.setVolume(0.42f)

        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                coordinator.setVolume(invalid)
            }
        }
        coordinator.toggleBlocking(gentleRain())

        assertEquals(0.42f, player.lastVolume)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
    }

    @Test
    fun publishedPlaybackIsADomainSummaryOfTheEngineState() = runBlocking {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Failed,
                isPlaying = true,
                isLooping = true,
                volume = 0.7f,
            )
        }
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        assertEquals(
            PlaybackSummary(
                activeSourceId = "gentle-rain",
                isPlaying = true,
                isLooping = true,
                volume = 0.7f,
                hasFailed = true,
            ),
            coordinator.playback.first(),
        )
    }

    @Test
    fun publishedPlaybackFallsBackToTheRequestedSourceWhileReconnecting() = runBlocking {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "calm-waves", uri = "file.mp3"),
                phase = PlaybackPhase.Loading,
            )
        }
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        assertEquals("calm-waves", coordinator.playback.first().activeSourceId)
    }

    @Test
    fun sleepTimerIsPublishedAsPlainRemainingMilliseconds() = runBlocking {
        val player = FakePlaybackController()
        val coordinator = DefaultPlaybackCoordinator(player, testContentResolver)

        assertEquals(null, coordinator.sleepTimerRemainingMs.first())

        player.mutableSleepTimerState.value = SleepTimerState(remainingMs = 90_000L)

        assertEquals(90_000L, coordinator.sleepTimerRemainingMs.first())
    }

    private fun gentleRain() = Music(
        id = "gentle-rain",
        name = "Rain on the Window",
        categoryId = "rain",
        durationSeconds = 9,
        playbackTitle = "Gentle Rain",
    )

    private fun calmWaves() = Music(
        id = "calm-waves",
        name = "Ontario Waves",
        categoryId = "ocean",
        durationSeconds = 286,
        playbackTitle = "Calm Waves",
    )

    private fun DefaultPlaybackCoordinator.toggleBlocking(music: Music) = runBlocking {
        togglePlayback(music)
    }

    /**
     * Both facades publish inside `submit`, before it returns, so a command the coordinator
     * sends is readable in `state` on the next line. This fake mirrors that for the fields the
     * coordinator reads back; tests still write [mutableState] directly to stage what only the
     * engine or a remote controller can cause.
     */
    private class FakePlaybackController : PlaybackController {
        val mutableState = MutableStateFlow(AudioPlayerState())
        override val state: StateFlow<AudioPlayerState> = mutableState
        val mutableSleepTimerState = MutableStateFlow(SleepTimerState())
        override val sleepTimerState: StateFlow<SleepTimerState> = mutableSleepTimerState
        var lastLoadRequest: PlaybackRequest? = null
        var playCalls = 0
        var pauseCalls = 0
        var lastLooping: Boolean? = null
        var lastVolume: Float? = null
        var lastSleepTimerDurationMs: Long? = null
        var cancelSleepTimerCalls = 0

        /** The engine finished loading and attached the source the app asked for. */
        fun attachRequestedSource() {
            mutableState.update { it.copy(source = it.requestedSource) }
        }

        override fun submit(command: PlaybackCommand) {
            when (command) {
                is PlaybackCommand.Load -> {
                    lastLoadRequest = command.request
                    mutableState.update {
                        it.copy(
                            requestedSource = command.request.source,
                            source = null,
                            playRequested = command.request.autoplay,
                            isLooping = command.request.loopMode == LoopMode.One,
                            volume = command.request.volume,
                        )
                    }
                }
                PlaybackCommand.Play -> {
                    playCalls++
                    mutableState.update { it.copy(playRequested = true) }
                }
                PlaybackCommand.Pause -> {
                    pauseCalls++
                    mutableState.update { it.copy(playRequested = false) }
                }
                is PlaybackCommand.SetLooping -> {
                    lastLooping = command.enabled
                    mutableState.update { it.copy(isLooping = command.enabled) }
                }
                is PlaybackCommand.SetVolume -> {
                    lastVolume = command.volume
                    mutableState.update { it.copy(volume = command.volume) }
                }
                is PlaybackCommand.StartSleepTimer -> lastSleepTimerDurationMs = command.durationMs
                PlaybackCommand.CancelSleepTimer -> cancelSleepTimerCalls++
            }
        }

        override fun release() = Unit
    }
}
