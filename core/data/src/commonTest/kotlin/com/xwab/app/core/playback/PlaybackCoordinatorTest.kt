package com.xwab.app.core.playback

import com.xwab.app.core.media.AudioPlayerState
import com.xwab.app.core.media.PlaybackCommand
import com.xwab.app.core.media.PlaybackController
import com.xwab.app.core.media.AudioSource
import com.xwab.app.core.media.LoopMode
import com.xwab.app.core.media.PlaybackPhase
import com.xwab.app.core.media.PlaybackRequest
import com.xwab.app.core.media.SleepTimerState
import com.xwab.app.core.model.Music
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class git statusPlaybackCoordinatorTest {
    @Test
    fun toggleReloadsFailedRequestedSourceWithAutoplay() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Failed,
            )
        }
        val repository = DefaultPlaybackCoordinator(player, ::testUri)

        repository.toggle(gentleRain())

        assertEquals(true, player.lastLoadRequest?.autoplay)
        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    @Test
    fun togglePausesOrPlaysTheActiveSourceWithoutReloading() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = true,
                isPlaying = true,
            )
        }
        val repository = DefaultPlaybackCoordinator(player, ::testUri)

        repository.toggle(gentleRain())

        assertEquals(1, player.pauseCalls)
        assertEquals(null, player.lastLoadRequest)
    }

    @Test
    fun togglePausesDesiredPlaybackWhileAutoplayIsBuffering() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Buffering,
                playRequested = true,
                isPlaying = false,
            )
        }
        val repository = DefaultPlaybackCoordinator(player, ::testUri)

        repository.toggle(gentleRain())

        assertEquals(1, player.pauseCalls)
        assertEquals(0, player.playCalls)
        assertEquals(null, player.lastLoadRequest)
    }

    @Test
    fun togglePlaysActiveSourceWhenPlaybackIsNotDesired() {
        val player = FakePlaybackController().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = false,
                isPlaying = false,
            )
        }
        val repository = DefaultPlaybackCoordinator(player, ::testUri)

        repository.toggle(gentleRain())

        assertEquals(1, player.playCalls)
        assertEquals(0, player.pauseCalls)
        assertEquals(null, player.lastLoadRequest)
    }

    @Test
    fun playbackSettingsAreRetainedWhenAnotherSoundIsLoaded() {
        val player = FakePlaybackController()
        val repository = DefaultPlaybackCoordinator(player, ::testUri)

        repository.setLooping(false)
        repository.setVolume(0.42f)
        repository.toggle(gentleRain())

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
        assertEquals(false, player.lastLooping)
        assertEquals(0.42f, player.lastVolume)
    }

    @Test
    fun sleepTimerCommandsAreForwardedToThePlayer() {
        val player = FakePlaybackController()
        val repository = DefaultPlaybackCoordinator(player, ::testUri)

        repository.startSleepTimer(15 * 60_000L)
        repository.cancelSleepTimer()

        assertEquals(15 * 60_000L, player.lastSleepTimerDurationMs)
        assertEquals(1, player.cancelSleepTimerCalls)
    }

    @Test
    fun nonFiniteVolumeIsRejectedWithoutPoisoningTheNextLoad() {
        val player = FakePlaybackController()
        val repository = DefaultPlaybackCoordinator(player, ::testUri)
        repository.setVolume(0.42f)

        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { invalid ->
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                repository.setVolume(invalid)
            }
        }
        repository.toggle(gentleRain())

        assertEquals(0.42f, player.lastVolume)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
    }

    private fun gentleRain() = Music(
        id = "gentle-rain",
        name = "Rain on the Window",
        categoryId = "rain",
        durationSeconds = 9,
        audioResource = "files/audio/gentle_rain.mp3",
        playbackTitle = "Gentle Rain",
    )

    private fun testUri(resourcePath: String): String = "test://$resourcePath"

    private class FakePlaybackController : PlaybackController {
        val mutableState = MutableStateFlow(AudioPlayerState())
        override val state: StateFlow<AudioPlayerState> = mutableState
        private val mutableSleepTimerState = MutableStateFlow(SleepTimerState())
        override val sleepTimerState: StateFlow<SleepTimerState> = mutableSleepTimerState
        var lastLoadRequest: PlaybackRequest? = null
        var playCalls = 0
        var pauseCalls = 0
        var lastLooping: Boolean? = null
        var lastVolume: Float? = null
        var lastSleepTimerDurationMs: Long? = null
        var cancelSleepTimerCalls = 0

        override fun submit(command: PlaybackCommand) {
            when (command) {
                is PlaybackCommand.Load -> lastLoadRequest = command.request
                PlaybackCommand.Play -> playCalls++
                PlaybackCommand.Pause -> pauseCalls++
                is PlaybackCommand.SetLooping -> lastLooping = command.enabled
                is PlaybackCommand.SetVolume -> lastVolume = command.volume
                is PlaybackCommand.StartSleepTimer -> lastSleepTimerDurationMs = command.durationMs
                PlaybackCommand.CancelSleepTimer -> cancelSleepTimerCalls++
            }
        }

        override fun release() = Unit
    }
}
