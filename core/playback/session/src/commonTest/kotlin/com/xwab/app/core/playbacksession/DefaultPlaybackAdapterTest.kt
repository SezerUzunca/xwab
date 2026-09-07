package com.xwab.app.core.playbacksession

import com.xwab.app.core.playback.port.DEFAULT_LOOPING
import com.xwab.app.core.playback.port.PlaybackFailure
import com.xwab.app.core.playback.port.PlaybackItemId
import com.xwab.app.core.playback.port.PlaybackSummary
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.sounddelivery.port.SoundContentResolution
import com.xwab.app.core.sound.port.Category
import com.xwab.app.core.sound.port.CategoryId
import com.xwab.app.core.sound.port.Music
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.sound.port.TrackId
import com.xwab.app.core.playbackengine.port.AudioPlayerState
import com.xwab.app.core.playbackengine.port.AudioSource
import com.xwab.app.core.playbackengine.port.LoopMode
import com.xwab.app.core.playbackengine.port.PlaybackCommand
import com.xwab.app.core.playbackengine.port.PlaybackEnginePort
import com.xwab.app.core.playbackengine.port.PlaybackPhase
import com.xwab.app.core.playbackengine.port.PlaybackRequest
import com.xwab.app.core.playbackengine.port.SleepTimerState
import com.xwab.app.core.story.port.Story
import com.xwab.app.core.story.port.StoryCatalogPort
import com.xwab.app.core.story.port.StoryId
import com.xwab.app.core.storysource.port.StorySourcePort
import com.xwab.app.core.storysource.port.StoryStreamSource
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

class DefaultPlaybackAdapterTest {
    private val testContentResolver =
        SoundContentPort { musicId -> SoundContentResolution.Resolved("test://$musicId") }

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
        val adapter = adapter(player) {
            lookupStarted.complete(Unit)
            SoundContentResolution.Resolved(lookupResult.await())
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

    @Test
    fun pauseAbandonsASourceLookupStillInFlight() = runBlocking {
        val lookupStarted = CompletableDeferred<Unit>()
        val lookupResult = CompletableDeferred<String>()
        val player = FakePlaybackEnginePort()
        val adapter = adapter(player) {
            lookupStarted.complete(Unit)
            SoundContentResolution.Resolved(lookupResult.await())
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
    fun theFirstSoundLoopsBecauseThatIsTheProductDefault() = runBlocking {
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
        val adapter = adapter(player) { musicId ->
            when (musicId) {
                TrackId("gentle-rain") -> {
                    rainStarted.complete(Unit)
                    SoundContentResolution.Resolved(rainResult.await())
                }
                TrackId("calm-waves") -> {
                    wavesStarted.complete(Unit)
                    SoundContentResolution.Resolved(wavesResult.await())
                }
                else -> SoundContentResolution.NotFound
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
        val adapter = adapter(player) { SoundContentResolution.Unavailable("offline") }

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
        val adapter = adapter(player) {
            lookupStarted.complete(Unit)
            SoundContentResolution.Resolved(lookupResult.await())
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
        val adapter = adapter(player) { SoundContentResolution.Unavailable("offline") }

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
        val adapter = adapter(player) {
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
     * The upgrade path. On Android the media service outlives the app, so a session started by a
     * build that wrote bare track ids can still be attached when this one connects to it. Reading
     * `gentle-rain` as a sound is what lets playback carry on; without it the session would see a
     * different item, resolve it again and restart the sound under the listener.
     */
    @Test
    fun aServiceStillHoldingAPreNamespacingIdIsRecognisedAsTheSameSound() = runBlocking {
        val player = FakePlaybackEnginePort().apply {
            mutableState.value = AudioPlayerState(
                source = AudioSource(id = "gentle-rain", uri = "file.mp3"),
                phase = PlaybackPhase.Ready,
            )
        }
        val adapter = adapter(player)

        adapter.play(sound("gentle-rain"))

        assertEquals(1, player.playCalls)
        assertNull(player.lastLoadRequest, "the attached sound must not be reloaded")
        assertEquals(sound("gentle-rain"), adapter.playback.first().activeItemId)
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

        adapter.play(PlaybackItemId.story("night-came-slowly"))

        assertEquals(
            PlaybackFailure.ItemNotFound(PlaybackItemId.story("night-came-slowly")),
            adapter.playback.first().failure,
        )
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

        storyAdapter(player).play(PlaybackItemId.story("night-came-slowly"))

        assertEquals("story:night-came-slowly", player.lastLoadRequest?.source?.id)
        assertEquals("https://example.test/night.mp3", player.lastLoadRequest?.source?.uri)
        assertEquals("The Night Came Slowly", player.lastLoadRequest?.source?.title)
        assertEquals("Alan Davis Drake", player.lastLoadRequest?.source?.artist)
    }

    /** A sleep sound repeats until the timer stops it. A story that repeats has not ended. */
    @Test
    fun aStoryDoesNotLoopByDefaultWhereASoundDoes() = runBlocking {
        val player = FakePlaybackEnginePort()

        storyAdapter(player).play(PlaybackItemId.story("night-came-slowly"))

        assertEquals(LoopMode.Off, player.lastLoadRequest?.loopMode)
    }

    /** The listener meant it, whatever they switch to next. */
    @Test
    fun aLoopTheListenerTurnedOnSurvivesASwitchToAStory() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.setLooping(true)
        adapter.play(PlaybackItemId.story("night-came-slowly"))

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

        adapter.play(PlaybackItemId.story("an-idle-fellow"))

        assertEquals(
            PlaybackFailure.SourceUnavailable(PlaybackItemId.story("an-idle-fellow")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }

    @Test
    fun aStoryTheCatalogDoesNotHoldIsPublishedAsAFailure() = runBlocking {
        val player = FakePlaybackEnginePort()
        val adapter = storyAdapter(player)

        adapter.play(PlaybackItemId.story("no-such-story"))

        assertEquals(
            PlaybackFailure.ItemNotFound(PlaybackItemId.story("no-such-story")),
            adapter.playback.first().failure,
        )
        assertNull(player.lastLoadRequest)
    }

    /** One resolver per kind: a second would never run, and nothing would say which. */
    @Test
    fun twoResolversForOneKindAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            DefaultPlaybackAdapter(
                FakePlaybackEnginePort(),
                listOf(
                    SoundPlaybackResolver(FakeCatalog, testContentResolver),
                    SoundPlaybackResolver(FakeCatalog, testContentResolver),
                ),
            )
        }
    }

    private fun sound(value: String) = PlaybackItemId.sound(value)

    private fun adapter(
        player: FakePlaybackEnginePort,
        resolver: SoundContentPort = testContentResolver,
    ) = DefaultPlaybackAdapter(player, listOf(SoundPlaybackResolver(FakeCatalog, resolver)))

    /** The same session with both kinds wired, which is what the app ships. */
    private fun storyAdapter(player: FakePlaybackEnginePort) = DefaultPlaybackAdapter(
        player,
        listOf(
            SoundPlaybackResolver(FakeCatalog, testContentResolver),
            StoryPlaybackResolver(FakeStoryCatalog, FakeStoryStreams),
        ),
    )

    /** Two catalog rows, with one source deliberately omitted to exercise defensive handling. */
    private object FakeStoryCatalog : StoryCatalogPort {
        private val stories = listOf(
            story("night-came-slowly", "The Night Came Slowly"),
            story("an-idle-fellow", "An Idle Fellow"),
        )

        override fun observeStories(): Flow<List<Story>> = flowOf(stories)
        override fun observeStory(storyId: StoryId): Flow<Story?> =
            flowOf(stories.find { it.id == storyId })

        private fun story(id: String, title: String) = Story(
            id = StoryId(id),
            title = title,
            author = "Kate Chopin",
            description = "A literary short story.",
            narrator = "Alan Davis Drake",
            durationSeconds = 174,
            artworkUrl = null,
        )
    }

    private object FakeStoryStreams : StorySourcePort {
        override fun sourceFor(storyId: StoryId): StoryStreamSource? =
            if (storyId == StoryId("night-came-slowly")) {
                StoryStreamSource("https://example.test/night.mp3")
            } else {
                null
            }
    }

    /** The catalog the adapter reads its metadata from; only `observeMusic` is ever asked. */
    private object FakeCatalog : SoundCatalogPort {
        private val tracks = listOf(
            Music(
                id = TrackId("gentle-rain"),
                name = "Rain on the Window",
                categoryId = CategoryId("rain"),
                durationSeconds = 9,
                playbackTitle = "Gentle Rain",
            ),
            Music(
                id = TrackId("calm-waves"),
                name = "Ontario Waves",
                categoryId = CategoryId("ocean"),
                durationSeconds = 286,
                playbackTitle = "Calm Waves",
            ),
        )

        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observeAllMusic(): Flow<List<Music>> = flowOf(tracks)
        override fun observeCategory(categoryId: CategoryId): Flow<Category?> = flowOf(null)
        override fun observeMusicForCategory(categoryId: CategoryId): Flow<List<Music>> = flowOf(emptyList())
        override fun observeMusic(trackId: TrackId): Flow<Music?> = flowOf(tracks.find { it.id == trackId })
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
