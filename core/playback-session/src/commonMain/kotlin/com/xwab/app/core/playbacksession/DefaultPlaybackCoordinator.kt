package com.xwab.app.core.playbacksession

import com.xwab.app.core.audiodelivery.resolution.AudioContentResolver
import com.xwab.app.core.audiodelivery.resolution.AudioSourceResolution
import com.xwab.app.core.catalog.Music
import com.xwab.app.core.catalog.MusicCatalogRepository
import com.xwab.app.core.catalog.TrackId
import com.xwab.app.core.playbackengine.api.AudioPlayerState
import com.xwab.app.core.playbackengine.api.AudioSource
import com.xwab.app.core.playbackengine.api.LoopMode
import com.xwab.app.core.playbackengine.api.PlaybackCommand
import com.xwab.app.core.playbackengine.api.PlaybackController
import com.xwab.app.core.playbackengine.api.PlaybackPhase
import com.xwab.app.core.playbackengine.api.PlaybackRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

internal class DefaultPlaybackCoordinator(
    private val controller: PlaybackController,
    private val contentResolver: AudioContentResolver,
    private val musicCatalog: MusicCatalogRepository,
) : PlaybackCoordinator {
    /**
     * What the session wants, which the engine cannot hold on its own.
     *
     * The engine only knows about a track once it has been handed a URI, so everything between a
     * tap and that moment — the track being resolved, and a resolution that came back empty —
     * lives here and is combined into the published summary.
     */
    private val intent = MutableStateFlow(SessionIntent())

    /**
     * Mapping to the domain summary here, rather than in a use case, is what keeps the engine's
     * state model inside this module. `combine` over two StateFlows still hands every new collector
     * the current pair, so nothing downstream has to wait for the next engine update.
     */
    override val playback: Flow<PlaybackSummary> =
        combine(controller.state, intent) { engine, wanted -> summaryOf(engine, wanted) }

    override val sleepTimerRemainingMs: Flow<Long?> = controller.sleepTimerState.map { it.remainingMs }

    /**
     * The controller owns loop and volume: it reconciles them across the engine, a remote
     * controller and reconnects, then publishes the result. The one thing it cannot know is
     * that sleep sounds are meant to loop — its own default is "no loop". This flag marks the
     * point where [DEFAULT_LOOPING] stops applying because a real preference exists to read.
     */
    private var loopPreferenceEstablished = false

    override suspend fun play(trackId: TrackId) {
        val engine = controller.state.value
        if (engine.activeSource?.id == trackId.value && engine.phase != PlaybackPhase.Failed) {
            // The engine is already holding this track's source; there is nothing to resolve.
            intent.update { it.superseded() }
            controller.submit(PlaybackCommand.Play)
            return
        }

        // Claimed before the lookup starts. Without this the session still looked idle while a
        // source was being resolved, so a second tap on the same track took this same branch and
        // resolved it again — two taps, and the net effect was Play rather than play-then-pause.
        val generation = intent.updateAndGet { it.superseded().copy(pendingTrackId = trackId) }.generation

        val music = musicCatalog.observeMusic(trackId).first()
            ?: return settle(generation, PlaybackFailure.TrackNotFound)

        when (val resolution = contentResolver.resolve(trackId)) {
            is AudioSourceResolution.Resolved -> {
                // A newer play() or a pause() arrived while the lookup was running; its own state
                // is the current one, and loading now would undo what the listener last asked for.
                if (intent.value.generation != generation) return
                controller.submit(PlaybackCommand.Load(loadRequest(music, resolution.uri)))
                settle(generation, failure = null)
            }
            AudioSourceResolution.NotFound -> settle(generation, PlaybackFailure.TrackNotFound)
            is AudioSourceResolution.Unavailable -> settle(generation, PlaybackFailure.SourceUnavailable)
        }
    }

    override fun pause() {
        // Superseding is what makes this reach a play() still waiting on a lookup: that lookup
        // completes into a session which has moved on, sees a stale generation, and loads nothing.
        intent.update { it.superseded() }
        controller.submit(PlaybackCommand.Pause)
    }

    override fun setLooping(enabled: Boolean) {
        loopPreferenceEstablished = true
        controller.submit(PlaybackCommand.SetLooping(enabled))
    }

    override fun setVolume(volume: Float) {
        require(volume.isFinite()) { "Volume must be finite." }
        controller.submit(PlaybackCommand.SetVolume(volume.coerceIn(0.0f, 1.0f)))
    }

    override fun startSleepTimer(durationMs: Long) {
        controller.submit(PlaybackCommand.StartSleepTimer(durationMs))
    }

    override fun cancelSleepTimer() {
        controller.submit(PlaybackCommand.CancelSleepTimer)
    }

    /** Releases the pending claim, unless a newer request has already taken the session over. */
    private fun settle(generation: Long, failure: PlaybackFailure?) {
        intent.update {
            if (it.generation == generation) it.copy(pendingTrackId = null, failure = failure) else it
        }
    }

    /**
     * The one place a [TrackId] is unwrapped on the way out: `core:playback-engine` is a standalone
     * library and its [AudioSource] identifies a source by plain string, as it should.
     */
    private fun loadRequest(music: Music, resolvedUri: String): PlaybackRequest = PlaybackRequest(
        source = AudioSource(music.id.value, resolvedUri, music.playbackTitle, music.playbackArtist),
        autoplay = true,
        loopMode = if (controller.state.value.effectiveLooping()) LoopMode.One else LoopMode.Off,
        volume = controller.state.value.volume,
    )

    private fun summaryOf(engine: AudioPlayerState, wanted: SessionIntent): PlaybackSummary {
        val playIntent = wanted.pendingTrackId != null || engine.playRequested
        return PlaybackSummary(
            trackId = wanted.pendingTrackId ?: engine.activeSource?.id?.let(::TrackId),
            playIntent = playIntent,
            isPlaying = engine.isPlaying,
            isPreparing = playIntent && !engine.isPlaying && engine.phase != PlaybackPhase.Failed,
            isLooping = engine.effectiveLooping(),
            volume = engine.volume,
            failure = wanted.failure ?: engine.engineFailure(),
        )
    }

    /**
     * The session's loop setting, carried into the next sound and published to screens from the
     * same call — so the value a listener sees before the first load is the value that load uses.
     *
     * The test is [AudioPlayerState.activeSource], not `source`: a dropped service connection
     * clears only the *attached* source while the session and its reconciled settings live on,
     * so keying off `source` would silently undo the listener's choice on the next sound.
     */
    private fun AudioPlayerState.effectiveLooping(): Boolean =
        if (loopPreferenceEstablished || activeSource != null) isLooping else DEFAULT_LOOPING

    private fun AudioPlayerState.engineFailure(): PlaybackFailure? =
        if (phase == PlaybackPhase.Failed) PlaybackFailure.EngineFailed else null

    /**
     * @param generation raised by every listener action, so a source lookup can tell on completion
     *   whether the session still wants what it went to fetch.
     * @param pendingTrackId a track whose source is being resolved: the session is on it before the
     *   engine is.
     */
    private data class SessionIntent(
        val generation: Long = 0L,
        val pendingTrackId: TrackId? = null,
        val failure: PlaybackFailure? = null,
    ) {
        /** A new listener action: whatever was in flight no longer counts, and neither does a failure. */
        fun superseded(): SessionIntent =
            copy(generation = generation + 1, pendingTrackId = null, failure = null)
    }
}
