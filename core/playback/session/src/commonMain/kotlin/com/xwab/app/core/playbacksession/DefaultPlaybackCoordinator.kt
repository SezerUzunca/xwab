package com.xwab.app.core.playbacksession

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

/**
 * The one session, over whatever kind of thing is playing.
 *
 * It knows nothing about sounds or stories itself: what an item is, where its bytes come from and
 * whether it should loop are a [PlaybackItemResolver]'s answers. What lives here is everything that
 * is true of a session regardless of content — one item at a time, the newest request wins, a claim
 * that lasts no longer than the request that made it, and a summary a screen can draw.
 */
internal class DefaultPlaybackCoordinator(
    private val controller: PlaybackController,
    resolvers: List<PlaybackItemResolver>,
) : PlaybackCoordinator {
    private val resolversByKind: Map<PlaybackKind, PlaybackItemResolver> =
        resolvers.associateBy { it.kind }

    init {
        // Two resolvers for one kind means one of them silently never runs.
        require(resolversByKind.size == resolvers.size) {
            "One resolver per playback kind: ${resolvers.map { it.kind }}"
        }
    }

    /**
     * What the session wants, which the engine cannot hold on its own.
     *
     * The engine only knows about an item once it has been handed a URI, so everything between a
     * tap and that moment — the item being resolved, and a resolution that came back empty —
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
     * controller and reconnects, then publishes the result. The one thing it cannot know is what
     * looping should mean for the item being loaded. This flag marks the point where the default —
     * the session's [DEFAULT_LOOPING], or the item's own — stops applying because a real preference
     * exists to read.
     */
    private var loopPreferenceEstablished = false

    override suspend fun play(itemId: PlaybackItemId) {
        val engine = controller.state.value
        if (itemOf(engine.activeSource) == itemId && engine.phase != PlaybackPhase.Failed) {
            // The engine is already holding this item's source; there is nothing to resolve.
            intent.update { it.superseded() }
            controller.submit(PlaybackCommand.Play)
            return
        }

        // Claimed before the lookup starts. Without this the session still looked idle while a
        // source was being resolved, so a second tap on the same item took this same branch and
        // resolved it again — two taps, and the net effect was Play rather than play-then-pause.
        val generation = intent.updateAndGet { it.superseded().copy(pendingItemId = itemId) }.generation

        try {
            // A kind nothing can resolve is a wiring gap rather than a listener error, and it is
            // reported as "nothing could find this" instead of pretending a source was unreachable.
            val resolver = resolversByKind[itemId.kind]
                ?: return settle(generation, PlaybackFailure.ItemNotFound(itemId))

            when (val resolution = resolver.resolve(itemId.value)) {
                is ItemResolution.Resolved -> {
                    // A newer play() or a pause() arrived while the lookup was running; its own
                    // state is the current one, and loading now would undo what was last asked for.
                    if (intent.value.generation != generation) return
                    controller.submit(PlaybackCommand.Load(loadRequest(itemId, resolution)))
                    settle(generation, failure = null)
                }
                ItemResolution.NotFound ->
                    settle(generation, PlaybackFailure.ItemNotFound(itemId))
                is ItemResolution.Unavailable ->
                    settle(generation, PlaybackFailure.SourceUnavailable(itemId))
            }
        } finally {
            // The claim outlives the coroutine that made it unless this runs. Callers launch into a
            // ViewModel scope, so leaving a screen mid-lookup cancels this — and the session is
            // app-scoped, so a claim left standing would report a phantom item as wanted and
            // preparing, on every screen, until the next tap. On the paths above this is a no-op:
            // `settle` has already released it. Cancellation still propagates.
            releaseClaim(generation)
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

    /** Releases the pending claim and records the outcome, unless a newer request has taken over. */
    private fun settle(generation: Long, failure: PlaybackFailure?) {
        intent.update {
            if (it.generation == generation) it.copy(pendingItemId = null, failure = failure) else it
        }
    }

    /**
     * Drops the claim without touching the outcome, so it is safe to run after [settle] has already
     * recorded one. Idempotent by generation: a newer request owns the session and keeps its claim.
     */
    private fun releaseClaim(generation: Long) {
        intent.update { if (it.generation == generation) it.copy(pendingItemId = null) else it }
    }

    /**
     * The one place an item id is flattened on the way out: `core:playback:engine` is a standalone
     * library and its [AudioSource] identifies a source by plain string, as it should. The kind
     * travels inside that string, so two items sharing a raw id stay two sources.
     */
    private fun loadRequest(
        itemId: PlaybackItemId,
        resolved: ItemResolution.Resolved,
    ): PlaybackRequest = PlaybackRequest(
        source = AudioSource(itemId.toEngineId(), resolved.uri, resolved.title, resolved.artist),
        autoplay = true,
        loopMode = if (controller.state.value.effectiveLooping(resolved.policy.defaultLooping)) {
            LoopMode.One
        } else {
            LoopMode.Off
        },
        volume = controller.state.value.volume,
    )

    private fun summaryOf(engine: AudioPlayerState, wanted: SessionIntent): PlaybackSummary {
        // What was asked for, and what is actually attached. They differ for the whole of a switch:
        // the listener has picked B while A is still the sound in the room.
        val requested = wanted.pendingItemId ?: itemOf(engine.activeSource)
        val active = itemOf(engine.source)
        val playIntent = wanted.pendingItemId != null || engine.playRequested

        return PlaybackSummary(
            requestedItemId = requested,
            activeItemId = active,
            playIntent = playIntent,
            isPlaying = engine.isPlaying,
            // About the *requested* item: a different sound being audible does not make the one
            // that was asked for ready.
            isPreparing = playIntent &&
                (requested != active || !engine.isPlaying) &&
                engine.phase != PlaybackPhase.Failed,
            isLooping = engine.effectiveLooping(),
            volume = engine.volume,
            failure = wanted.failure ?: engine.engineFailure(),
        )
    }

    /** The item an engine source names, reading a pre-namespacing id as the sound it was. */
    private fun itemOf(source: AudioSource?): PlaybackItemId? =
        source?.id?.let { playbackItemIdOf(it) }

    /**
     * The session's loop setting, carried into the next item and published to screens from the same
     * call — so the value a listener sees before the first load is the value that load uses.
     *
     * An explicit choice is session-wide and survives a switch, including one between kinds: a
     * listener who turned looping off meant it. Only [defaultLooping] is per-item, which is what
     * lets a story start un-looped without overruling a preference somebody did express.
     *
     * The test is [AudioPlayerState.activeSource], not `source`: a dropped service connection
     * clears only the *attached* source while the session and its reconciled settings live on,
     * so keying off `source` would silently undo the listener's choice on the next item.
     */
    private fun AudioPlayerState.effectiveLooping(defaultLooping: Boolean = DEFAULT_LOOPING): Boolean =
        if (loopPreferenceEstablished || activeSource != null) isLooping else defaultLooping

    private fun AudioPlayerState.engineFailure(): PlaybackFailure? =
        if (phase == PlaybackPhase.Failed) {
            itemOf(activeSource)?.let { PlaybackFailure.EngineFailed(it) }
        } else {
            null
        }

    /**
     * @param generation raised by every listener action, so a source lookup can tell on completion
     *   whether the session still wants what it went to fetch.
     * @param pendingItemId an item whose source is being resolved: the session is on it before the
     *   engine is.
     */
    private data class SessionIntent(
        val generation: Long = 0L,
        val pendingItemId: PlaybackItemId? = null,
        val failure: PlaybackFailure? = null,
    ) {
        /** A new listener action: whatever was in flight no longer counts, and neither does a failure. */
        fun superseded(): SessionIntent =
            copy(generation = generation + 1, pendingItemId = null, failure = null)
    }
}
