@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.session.port.DEFAULT_LOOPING
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackPolicy
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.LoopMode
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.PlaybackError
import com.xwab.app.core.playback.port.PlaybackErrorCode
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.port.PlaybackRequest
import com.xwab.app.core.playback.port.SleepTimerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout

class DefaultPlaybackAdapterTest {
    @Test
    fun turningLoopOffBeforeLoadingNotifiesAnExistingCollector() = runBlocking {
        val session = adapter(FakePlaybackEnginePort())
        val values = Channel<Boolean>(Channel.UNLIMITED)
        val collector = launch { session.playback.collect { values.send(it.isLooping) } }
        try {
            assertEquals(true, withTimeout(1_000) { values.receive() })
            session.setLooping(false)
            assertEquals(false, withTimeout(1_000) { values.receive() })
        } finally {
            collector.cancelAndJoin()
            values.close()
        }
    }

    /**
     * How the session is told what an item turns out to be.
     *
     * The kinds below are this test's own strings. `:core:session` depends on no content module, so
     * there is nothing here to borrow one from — and that absence is the point: the session resolves
     * whatever it is handed by looking the kind up in the map it was given, and these fakes are as
     * much of a content type as it ever sees.
     */
    private fun resolvedSound(value: String, uri: String): ItemResolution =
        ItemResolution.Resolved(
            uri = uri,
            title = SOUND_TITLES.getValue(value),
            displayName = SOUND_NAMES.getValue(value),
            artist = "Sleep Sounds",
            policy = PlaybackPolicy(defaultLooping = true),
        )

    @Test
    fun playReloadsAFailedSourceWithAutoplay() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Failed,
                isLooping = true,
            )
        }
        val adapter = adapter(player)

        adapter.play(sound("gentle-rain"))

        assertEquals(true, player.lastLoadRequest?.autoplay)
        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    @Test
    fun pauseStopsTheActiveSourceWithoutReloadingIt() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = true,
                isPlaying = true,
            )
        }
        val adapter = adapter(player)

        adapter.pause()

        assertEquals(1, player.pauseCalls)
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun playResumesTheActiveSourceWithoutReloadingIt() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = false,
                isPlaying = false,
            )
        }
        val adapter = adapter(player)

        adapter.play(sound("gentle-rain"))

        assertEquals(1, player.playCalls)
        assertEquals(0, player.pauseCalls)
        assertNull(player.lastLoadRequest)
    }

    /**
     * The contract that used to be broken: a buffering sound is not audible, so the screen drew a
     * Play icon, while the session decided from the desired state and paused on the next tap. Both
     * now read [PlaybackSummary.playIntent], so what the control shows is what a tap acts on.
     */
    @Test
    fun whatThePlayControlRendersIsWhatATapActsOn() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Buffering,
                playRequested = true,
                isPlaying = false,
            )
        }
        val adapter = adapter(player)

        val buffering = adapter.playback.first()

        assertEquals(false, buffering.isPlaying, "nothing is audible yet")
        assertTrue(buffering.playIntent, "but playback is wanted, which is what the control draws")
        assertTrue(buffering.isPreparing)
    }

    /**
     * The session is on a track from the tap onwards, not from the load onwards. Without that, two
     * quick taps on the same sound both saw an idle session, both resolved it, and the net effect
     * was Play — a listener double-tapping got no pause at all.
     */
    @Test
    fun theSessionIsOnATrackWhileItsSourceIsStillBeingResolved() = runBlocking {
        val lookupStarted = CompletableDeferred<Unit>()
        val lookupResult = CompletableDeferred<String>()
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) { value ->
            lookupStarted.complete(Unit)
            resolvedSound(value, lookupResult.await())
        }

        val firstTap = launch { adapter.play(sound("gentle-rain")) }
        lookupStarted.await()

        val whileResolving = adapter.playback.first()
        assertEquals(sound("gentle-rain"), whileResolving.requestedItemId)
        assertTrue(whileResolving.playIntent, "a second tap must find something to pause")
        assertTrue(whileResolving.isPreparing)

        lookupResult.complete("test://gentle-rain")
        firstTap.join()
    }

    /**
     * The name travels with the source the engine was handed, so the session does not have to ask
     * a catalog a second time — and the app shell, which draws the now-playing bar, never has to
     * ask one at all.
     */
    /**
     * The catalog's name, not the platform's. `gentle-rain` is listed as "Rain on the Window" and
     * announced in the notification as "Gentle Rain"; a now-playing bar two rows under the list
     * that named it has to agree with the list.
     */
    @Test
    fun theSummaryNamesTheItemTheWayTheCatalogDoes() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.play(sound("gentle-rain"))
        player.attachRequestedSource()

        assertEquals("Rain on the Window", adapter.playback.first().title)
        assertEquals(
            "Gentle Rain",
            player.lastLoadRequest?.source?.title,
            "the platform still gets the playback title",
        )
    }

    /**
     * A service that outlived the app comes back holding a source and nothing else. The session has
     * no name of its own for it, and the one the notification is already showing beats none.
     */
    @Test
    fun aReconnectedSourceIsNamedByWhatThePlatformKept() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3", title = "Gentle Rain"),
                phase = PlaybackPhase.Ready,
            )
        }

        assertEquals("Gentle Rain", adapter(player).playback.first().title)
    }

    /**
     * The whole reason the title is gated rather than published raw: for the length of a switch the
     * engine still holds A while the session has been asked for B. Publishing the engine's title
     * unconditionally would put "Rain on the Window" under a bar that is preparing Ontario Waves.
     */
    @Test
    fun noTitleIsPublishedWhileTheSessionIsSwitchingToAnotherItem() = runBlocking {
        val lookupStarted = CompletableDeferred<Unit>()
        val lookupResult = CompletableDeferred<String>()
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) { value ->
            if (value == "calm-waves") {
                lookupStarted.complete(Unit)
                resolvedSound(value, lookupResult.await())
            } else {
                resolvedSound(value, "test://$value")
            }
        }

        adapter.play(sound("gentle-rain"))
        player.attachRequestedSource()
        assertEquals("Rain on the Window", adapter.playback.first().title)

        val switch = launch { adapter.play(sound("calm-waves")) }
        lookupStarted.await()

        val whileSwitching = adapter.playback.first()
        assertEquals(sound("calm-waves"), whileSwitching.requestedItemId)
        assertTrue(whileSwitching.isPreparing)
        assertNull(
            whileSwitching.title,
            "the outgoing item's name must not stand in for the incoming one",
        )

        lookupResult.complete("test://calm-waves")
        switch.join()
        assertEquals("Ontario Waves", adapter.playback.first().title)
    }

    @Test
    fun pauseAbandonsASourceLookupStillInFlight() = runBlocking {
        val lookupStarted = CompletableDeferred<Unit>()
        val lookupResult = CompletableDeferred<String>()
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) { value ->
            lookupStarted.complete(Unit)
            resolvedSound(value, lookupResult.await())
        }

        val firstTap = launch { adapter.play(sound("gentle-rain")) }
        lookupStarted.await()
        adapter.pause()

        lookupResult.complete("test://gentle-rain")
        firstTap.join()

        assertNull(player.lastLoadRequest, "an abandoned lookup must not start playback")
        assertEquals(false, adapter.playback.first().playIntent)
    }

    @Test
    fun aSoundLoopsIndependentlyOfTheUnloadedSessionDefault() = runBlocking {
        val player = FakePlaybackEnginePort()

        adapter(player).play(sound("gentle-rain"))

        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    /**
     * The default has one owner. It used to have two — the session, deciding what to load, and the
     * player screen, which showed "looping" whenever no source was attached — so a loop turned off
     * before the first play was obeyed by the engine and denied by the UI.
     */
    @Test
    fun theLoopDefaultIsPublishedBeforeAnythingIsLoaded() = runBlocking {
        assertEquals(DEFAULT_LOOPING, adapter(FakePlaybackEnginePort()).playback.first().isLooping)
    }

    @Test
    fun aLoopTurnedOffBeforeTheFirstLoadIsPublishedAsOff() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.setLooping(false)

        assertEquals(false, adapter.playback.first().isLooping)
        adapter.play(sound("gentle-rain"))
        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
    }

    @Test
    fun settingsChosenBeforeTheFirstLoadBeatTheProductDefault() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.setLooping(false)
        adapter.setVolume(0.42f)
        adapter.play(sound("gentle-rain"))

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
        assertEquals(false, player.lastLooping)
        assertEquals(0.42f, player.lastVolume)
    }

    @Test
    fun playbackSettingsAreRetainedWhenAnotherSoundIsLoaded() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)
        adapter.play(sound("gentle-rain"))
        player.attachRequestedSource()

        adapter.setLooping(false)
        adapter.setVolume(0.42f)
        adapter.play(sound("calm-waves"))

        assertEquals("sound:calm-waves", player.lastLoadRequest?.source?.id)
        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
    }

    @Test
    fun settingsChangedOutsideTheAppSurviveALostServiceConnection() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)
        adapter.play(sound("gentle-rain"))
        player.attachRequestedSource()

        // A notification or Bluetooth control changes the settings behind the app's back;
        // the reducer adopts them, so they reach the adapter through the published state.
        player.mutableState.update { it.copy(isLooping = false, volume = 0.3f) }
        // The service connection then drops, which clears only the *attached* source.
        player.mutableState.update { it.copy(source = null) }

        adapter.play(sound("calm-waves"))

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
        assertEquals(0.3f, player.lastLoadRequest?.volume)
    }

    @Test
    fun anOlderSlowResolutionCannotReplaceTheUsersNewerSelection() = runBlocking {
        val rainStarted = CompletableDeferred<Unit>()
        val wavesStarted = CompletableDeferred<Unit>()
        val rainResult = CompletableDeferred<String>()
        val wavesResult = CompletableDeferred<String>()
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) { value ->
            when (value) {
                "gentle-rain" -> {
                    rainStarted.complete(Unit)
                    resolvedSound(value, rainResult.await())
                }
                "calm-waves" -> {
                    wavesStarted.complete(Unit)
                    resolvedSound(value, wavesResult.await())
                }
                else -> ItemResolution.Unavailable("missing source")
            }
        }

        val olderRequest = launch { adapter.play(sound("gentle-rain")) }
        rainStarted.await()
        val newerRequest = launch { adapter.play(sound("calm-waves")) }
        wavesStarted.await()

        wavesResult.complete("https://example.test/waves.mp3")
        newerRequest.join()
        rainResult.complete("https://example.test/rain.mp3")
        olderRequest.join()

        assertEquals("sound:calm-waves", player.lastLoadRequest?.source?.id)
    }

    /**
     * The metadata a media session publishes is read beside the source it is paired with, rather
     * than handed in by a screen — which could pair a stale title with a fresh URI unnoticed.
     */
    @Test
    fun theMediaSessionMetadataIsReadFromTheCatalog() = runBlocking {
        val player = FakePlaybackEnginePort()

        adapter(player).play(sound("gentle-rain"))

        assertEquals("Gentle Rain", player.lastLoadRequest?.source?.title)
        assertEquals("Sleep Sounds", player.lastLoadRequest?.source?.artist)
    }

    /**
     * A resolution that came back empty used to be dropped where it happened, so a listener tapped
     * and nothing at all occurred — no sound, and no reason given.
     */
    @Test
    fun aTrackTheCatalogDoesNotHoldIsPublishedAsAFailure() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.play(sound("no-such-track"))

        assertEquals(
            PlaybackFailure.ItemNotFound(sound("no-such-track")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun aSourceThatCouldNotBeReachedIsPublishedAsAFailure() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) { ItemResolution.Unavailable("offline") }

        adapter.play(sound("gentle-rain"))

        assertEquals(
            PlaybackFailure.SourceUnavailable(sound("gentle-rain")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun actingAgainClearsTheFailureTheListenerJustActedPast() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)
        adapter.play(sound("no-such-track"))

        adapter.play(sound("gentle-rain"))

        assertNull(adapter.playback.first().failure)
        assertEquals("sound:gentle-rain", player.lastLoadRequest?.source?.id)
    }

    @Test
    fun sleepTimerCommandsAreForwardedToThePlayer() {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.startSleepTimer(15 * 60_000L)
        adapter.cancelSleepTimer()

        assertEquals(15 * 60_000L, player.lastSleepTimerDurationMs)
        assertEquals(1, player.cancelSleepTimerCalls)
    }

    /**
     * The port states a range, so the range is this adapter's to keep — in both directions.
     *
     * It was kept on the way in and not on the way out, and untested either way, so the one screen
     * that renders a volume clamped it again for itself. A second screen would have had to know to
     * do the same.
     */
    @Test
    fun aVolumeOutsideTheRangeIsClampedGoingBothWays() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.setVolume(1.4f)
        assertEquals(1.0f, player.lastVolume)
        adapter.setVolume(-0.2f)
        assertEquals(0.0f, player.lastVolume)

        // An engine reporting its own idea of loudness does not get to break the published range.
        player.mutableState.update { it.copy(volume = 1.4f) }
        assertEquals(1.0f, adapter.playback.first().volume)
        player.mutableState.update { it.copy(volume = -0.2f) }
        assertEquals(0.0f, adapter.playback.first().volume)
    }

    /**
     * The load path has a sharper edge than the summary: a `PlaybackRequest` *refuses* a volume
     * outside the range rather than rounding it. So an engine reporting its own idea of loudness
     * would not show up as a wrong number — it would throw, on the next sound the listener asked
     * for, for a reason nothing they did explains.
     */
    @Test
    fun anEngineReportingAnOutOfRangeVolumeDoesNotBreakTheNextLoad() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)
        player.mutableState.update { it.copy(volume = 1.4f) }

        adapter.play(sound("gentle-rain"))

        assertEquals(1.0f, player.lastLoadRequest?.volume)
    }

    @Test
    fun nonFiniteVolumeIsRejectedWithoutPoisoningTheNextLoad() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)
        adapter.setVolume(0.42f)

        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                adapter.setVolume(invalid)
            }
        }
        adapter.play(sound("gentle-rain"))

        assertEquals(0.42f, player.lastVolume)
        assertEquals(0.42f, player.lastLoadRequest?.volume)
    }

    @Test
    fun publishedPlaybackIsADomainSummaryOfTheEngineState() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Failed,
                playRequested = true,
                isPlaying = true,
                isLooping = true,
                volume = 0.7f,
            )
        }

        assertEquals(
            PlaybackSummary(
                requestedItemId = sound("gentle-rain"),
                activeItemId = sound("gentle-rain"),
                playIntent = true,
                isPlaying = true,
                isPreparing = false,
                isLooping = true,
                volume = 0.7f,
                failure = PlaybackFailure.EngineFailed(sound("gentle-rain")),
            ),
            adapter(player).playback.first(),
        )
    }

    /**
     * The ordinary case is a listener with no network: the engine is handed a source it can never
     * open. That is a reach failure, and the one answer worth giving is the one that says trying
     * again might work — so it must not arrive as the engine having broken on a source it held.
     */
    @Test
    fun aSourceTheEngineCouldNeverOpenIsReportedAsOutOfReach() = runBlocking {
        listOf(PlaybackErrorCode.InvalidSource, PlaybackErrorCode.Timeout).forEach { code ->
            val player = FakePlaybackEnginePort().apply {
                mutableState.value = AudioPlayerState(
                    source = AudioSource(id = "sound:gentle-rain", uri = "https://example.test/x.mp3"),
                    phase = PlaybackPhase.Failed,
                    error = PlaybackError(code),
                )
            }

            assertEquals(
                PlaybackFailure.SourceUnavailable(sound("gentle-rain")),
                adapter(player).playback.first().failure,
                "$code is a source that was never opened",
            )
        }
    }

    /** The other half of the same decision: a source it had accepted and then broke on. */
    @Test
    fun aSourceTheEngineBrokeOnIsStillAnEngineFailure() = runBlocking {
        listOf(PlaybackErrorCode.PlaybackFailed, PlaybackErrorCode.ServiceUnavailable).forEach { code ->
            val player = FakePlaybackEnginePort().apply {
                mutableState.value = AudioPlayerState(
                    source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                    phase = PlaybackPhase.Failed,
                    error = PlaybackError(code),
                )
            }

            assertEquals(
                PlaybackFailure.EngineFailed(sound("gentle-rain")),
                adapter(player).playback.first().failure,
                "$code is the engine failing, not the source being out of reach",
            )
        }
    }

    /**
     * A dropped service connection clears only the *attached* source. The session's own choice has
     * to survive it, or a screen would blank out mid-reconnect — so the request lives on while the
     * active track goes null, which is exactly what the two fields are for.
     */
    @Test
    fun publishedPlaybackFallsBackToTheRequestedSourceWhileReconnecting() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                requestedSource = AudioSource(id = "sound:calm-waves", uri = "file.mp3"),
                phase = PlaybackPhase.Loading,
            )
        }

        val summary = adapter(player).playback.first()

        assertEquals(sound("calm-waves"), summary.requestedItemId)
        assertNull(summary.activeItemId)
    }

    /**
     * The switch, which is where one id could never have been enough: B is what the listener asked
     * for, A is what the room can hear. Publishing `trackId = B, isPlaying = true` said B was
     * playing for as long as B took to resolve.
     */
    @Test
    fun aSwitchPublishesTheRequestedTrackAndTheAudibleOneSeparately() = runBlocking {
        val lookupStarted = CompletableDeferred<Unit>()
        val lookupResult = CompletableDeferred<String>()
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = true,
                isPlaying = true,
            )
        }
        val adapter = adapter(player) { value ->
            lookupStarted.complete(Unit)
            resolvedSound(value, lookupResult.await())
        }

        val switch = launch { adapter.play(sound("calm-waves")) }
        lookupStarted.await()

        val midSwitch = adapter.playback.first()
        assertEquals(sound("calm-waves"), midSwitch.requestedItemId, "what was asked for")
        assertEquals(sound("gentle-rain"), midSwitch.activeItemId, "what is audible")
        assertTrue(midSwitch.isPlaying, "the old sound has not stopped")
        assertTrue(midSwitch.isPreparing, "and the new one is not ready")

        lookupResult.complete("test://calm-waves")
        switch.join()
    }

    /**
     * A failure releases the claim that produced it, so the session falls back to whatever came
     * before. The failure therefore has to name its own track — a screen matching against the
     * session's *current* one saw nothing, which is how a resolution error reached no listener.
     */
    @Test
    fun aFailureNamesTheTrackItIsAboutEvenAfterTheSessionMovesOn() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "sound:gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
                playRequested = true,
                isPlaying = true,
            )
        }
        val adapter = adapter(player) { ItemResolution.Unavailable("offline") }

        adapter.play(sound("calm-waves"))

        val summary = adapter.playback.first()
        assertEquals(
            PlaybackFailure.SourceUnavailable(sound("calm-waves")),
            summary.failure,
            "the failure belongs to the track that failed",
        )
        assertEquals(sound("gentle-rain"), summary.requestedItemId, "the session fell back")
    }

    /**
     * Callers launch `play` into a ViewModel scope, and the session outlives every one of them.
     * A lookup cancelled by a screen going away used to leave its claim standing, so `playIntent`
     * and `isPreparing` read true for a phantom track on every other screen until the next tap.
     */
    @Test
    fun aCancelledLookupLeavesNoClaimBehind() = runBlocking {
        val lookupStarted = CompletableDeferred<Unit>()
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) { value ->
            lookupStarted.complete(Unit)
            awaitCancellation()
        }

        val abandoned = launch { adapter.play(sound("gentle-rain")) }
        lookupStarted.await()
        abandoned.cancelAndJoin()

        val summary = adapter.playback.first()
        assertNull(summary.requestedItemId, "the claim should not outlive the coroutine that made it")
        assertEquals(false, summary.playIntent)
        assertEquals(false, summary.isPreparing)
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun sleepTimerIsPublishedAsPlainRemainingMilliseconds() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        assertNull(adapter.sleepTimerRemainingMs.first())

        player.mutableSleepTimerState.value = SleepTimerState(remainingMs = 90_000L)

        assertEquals(90_000L, adapter.sleepTimerRemainingMs.first())
    }

    /**
     * What replaced the upgrade path.
     *
     * On Android the media service outlives the app, so a session started by an older build can
     * still be attached when this one connects. An id with no kind used to be read as a sound,
     * which kept that playback carrying on. With kinds open there is no module here that knows
     * which kind would be the one to guess, so the id names nothing and the item is loaded fresh
     * rather than carried on as something it may not be. Nothing is lost that was reachable: no
     * released build ever wrote a bare id.
     */
    @Test
    fun aServiceHoldingAnIdWithNoKindIsNotMistakenForTheItemBeingPlayed() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
            )
        }
        val adapter = adapter(player)

        adapter.play(sound("gentle-rain"))

        assertEquals(
            "sound:gentle-rain",
            player.lastLoadRequest?.source?.id,
            "an id with no kind names nothing, so there is nothing to carry on from",
        )
    }

    /**
     * The reason ids are namespaced at all: `forest` is a plausible name for both a sound and a
     * story, and an unqualified id would have made the session take this for the item it already
     * holds and send `Play` — playing a story where a sound was asked for.
     */
    @Test
    fun aSoundAndAStoryThatShareAnIdAreNotTheSameItem() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "story:gentle-rain", uri = "https://example.test/story.mp3"),
                phase = PlaybackPhase.Ready,
            )
        }
        val adapter = adapter(player)

        adapter.play(sound("gentle-rain"))

        assertEquals(0, player.playCalls, "a story of the same name is not this sound")
        assertEquals("sound:gentle-rain", player.lastLoadRequest?.source?.id)
    }

    /**
     * Stories have no resolver until something can say where one streams from. Until then a story
     * request has to fail where it is made — reaching the engine with an unresolved item is how a
     * wiring gap turns into a player that sits on a source it cannot open.
     */
    @Test
    fun anItemOfAKindNothingResolvesFailsWithoutReachingTheEngine() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player)

        adapter.play(story("night-came-slowly"))

        assertEquals(
            PlaybackFailure.ItemNotFound(story("night-came-slowly")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun aRemovedKindCannotResumeASourceRetainedByTheEngine() = runBlocking {
        val removedItem = PlaybackItemId("removed-content", "retained-item")
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = removedItem.toEngineId(), uri = "https://example.test/old.mp3"),
                phase = PlaybackPhase.Ready,
            )
        }
        val adapter = adapter(player)

        adapter.play(removedItem)

        assertEquals(PlaybackFailure.ItemNotFound(removedItem), adapter.playback.first().failure)
        assertEquals(0, player.playCalls)
        assertNull(player.lastLoadRequest)
    }

    /**
     * A story is resolved the same way a sound is, minus the cache: catalog for the title, stream
     * catalog for the address, straight to the engine. Nothing on this path can write to disk,
     * which is the one difference between the two kinds that is meant to stay.
     */
    @Test
    fun aStoryIsPlayedStraightFromItsStreamAddress() = runBlocking {
        val player = FakePlaybackEnginePort()

        storyAdapter(player).play(story("night-came-slowly"))

        assertEquals("story:night-came-slowly", player.lastLoadRequest?.source?.id)
        assertEquals("https://example.test/night.mp3", player.lastLoadRequest?.source?.uri)
        assertEquals("The Night Came Slowly", player.lastLoadRequest?.source?.title)
        assertEquals("Alan Davis Drake", player.lastLoadRequest?.source?.artist)
    }

    /** A sleep sound repeats until the timer stops it. A story that repeats has not ended. */
    @Test
    fun aStoryDoesNotLoopByDefaultWhereASoundDoes() = runBlocking {
        val player = FakePlaybackEnginePort()

        storyAdapter(player).play(story("night-came-slowly"))

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
    }

    /** The listener meant it, whatever they switch to next. */
    @Test
    fun aLoopTheListenerTurnedOnSurvivesASwitchToAStory() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.setLooping(true)
        adapter.play(story("night-came-slowly"))

        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    /**
     * Without an explicit preference, a sound playing first must not decide what a story plays as
     * next. Nothing here calls `setLooping` — only the sound's own default is engine state by the
     * time the story is requested, and that is not a preference.
     */
    @Test
    fun aSoundsLoopDoesNotLeakIntoAStoryPlayedNextWithoutAPreference() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.play(sound("gentle-rain"))
        adapter.play(story("night-came-slowly"))

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
    }

    /** The same gap in the other direction: a story's non-looping default must not carry to a sound. */
    @Test
    fun aStorysLoopDoesNotLeakIntoASoundPlayedNextWithoutAPreference() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.play(story("night-came-slowly"))
        adapter.play(sound("gentle-rain"))

        assertEquals(LoopMode.One, player.lastLoadRequest?.loopMode)
    }

    /**
     * The shipped manifest has a source for every story. A mismatched catalog/source implementation
     * still fails explicitly instead of sending an empty URI to the engine.
     */
    @Test
    fun aCatalogAndSourceMismatchIsUnavailableRatherThanMissing() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.play(story("an-idle-fellow"))

        assertEquals(
            PlaybackFailure.SourceUnavailable(story("an-idle-fellow")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun aStoryTheCatalogDoesNotHoldIsPublishedAsAFailure() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.play(story("no-such-story"))

        assertEquals(
            PlaybackFailure.ItemNotFound(story("no-such-story")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }


    private fun sound(value: String) = PlaybackItemId(SOUND, value)

    private fun story(value: String) = PlaybackItemId(STORY, value)

    /**
     * A session wired for sounds only, with the resolution step left swappable.
     *
     * The lambda is where a real resolver's work would happen — a catalog read, a cache lookup, a
     * download. None of that is this module's business any more, so the tests that used to stage a
     * slow or failing delivery stage a slow or failing *resolution* instead, which is the only
     * shape the session can actually see.
     */
    private fun adapter(
        player: FakePlaybackEnginePort,
        resolve: suspend (String) -> ItemResolution = ::defaultSoundResolution,
    ) = DefaultPlaybackAdapter(player, mapOf(SOUND to PlaybackItemResolver { resolve(it) }))

    /** The same session with a second kind wired, which is what the app ships. */
    private fun storyAdapter(player: FakePlaybackEnginePort) = DefaultPlaybackAdapter(
        player,
        mapOf(
            SOUND to PlaybackItemResolver { defaultSoundResolution(it) },
            STORY to PlaybackItemResolver { storyResolution(it) },
        ),
    )

    private fun defaultSoundResolution(value: String): ItemResolution =
        if (value in SOUND_NAMES) resolvedSound(value, "test://$value") else ItemResolution.NotFound

    /** One story resolves, one has no source, and anything else is not in the catalog at all. */
    private fun storyResolution(value: String): ItemResolution = when (value) {
        "night-came-slowly" -> ItemResolution.Resolved(
            uri = "https://example.test/night.mp3",
            title = "The Night Came Slowly",
            // A story is listed and announced under the same name; it has no second one.
            displayName = "The Night Came Slowly",
            artist = "Alan Davis Drake",
            policy = PlaybackPolicy(defaultLooping = false),
        )
        "an-idle-fellow" -> ItemResolution.Unavailable("story source is missing")
        else -> ItemResolution.NotFound
    }
    /**
     * Both facades publish inside `submit`, before it returns, so a command the adapter
     * sends is readable in `state` on the next line. This fake mirrors that for the fields the
     * adapter reads back; tests still write [mutableState] directly to stage what only the
     * engine or a remote controller can cause.
     */
    private class FakePlaybackEnginePort : PlaybackEnginePort {
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

/**
 * The kinds this test invents for itself.
 *
 * They read like the app's two content types because the assertions around engine ids do, but
 * nothing imports them from `:core:sound` or `:core:story` — `:core:session` cannot see either
 * module, which is the property the whole file is built on.
 */
private const val SOUND = "sound"
private const val STORY = "story"

/** What the lists show. Differs from the notification title, which is why both are carried. */
private val SOUND_NAMES = mapOf(
    "gentle-rain" to "Rain on the Window",
    "calm-waves" to "Ontario Waves",
)

/** What the platform media session publishes. */
private val SOUND_TITLES = mapOf(
    "gentle-rain" to "Gentle Rain",
    "calm-waves" to "Calm Waves",
)
