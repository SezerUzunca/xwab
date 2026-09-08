package com.xwab.app.core.session

import com.xwab.app.core.session.port.DEFAULT_LOOPING
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackKind
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.LoopMode
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.port.PlaybackRequest
import com.xwab.app.core.sound.port.SoundCatalogPort
import com.xwab.app.core.sounddelivery.port.SoundContentPort
import com.xwab.app.core.story.port.StoryCatalogPort
import com.xwab.app.core.storysource.port.StorySourcePort
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
internal class DefaultPlaybackAdapter internal constructor(
    private val enginePort: PlaybackEnginePort,
    resolvers: List<PlaybackItemResolver>,
) : PlaybackPort {
    @Inject
    internal constructor(
        enginePort: PlaybackEnginePort,
        soundCatalogPort: SoundCatalogPort,
        soundContentPort: SoundContentPort,
        storyCatalogPort: StoryCatalogPort,
        storySourcePort: StorySourcePort,
    ) : this(
        enginePort = enginePort,
        resolvers = listOf(
            SoundPlaybackResolver(catalog = soundCatalogPort, content = soundContentPort),
            StoryPlaybackResolver(catalog = storyCatalogPort, streams = storySourcePort),
        ),
    )
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
        combine(enginePort.state, intent) { engine, wanted -> summaryOf(engine, wanted) }

    override val sleepTimerRemainingMs: Flow<Long?> = enginePort.sleepTimerState.map { it.remainingMs }

    /**
     * The controller owns loop and volume: it reconciles them across the engine, a remote
     * controller and reconnects, then publishes the result. The one thing it cannot know is what
     * looping should mean for the item being loaded. This flag marks the point where the default —
     * the session's [DEFAULT_LOOPING], or the item's own — stops applying because a real preference
     * exists to read.
     */
    private var loopPreferenceEstablished = false

    /**
     * The loop value this adapter itself last sent as part of a [PlaybackCommand.Load], or `null`
     * before the first one.
     *
     * The only way to tell a genuine preference — one adopted from a remote controller, which never
     * runs through [setLooping] — apart from a value that is merely sitting there because the last
     * item's own default put it there: compare the engine's current `isLooping` against this. Equal
     * means nothing has touched it since; different means something did, most likely a remote
     * control, and that counts exactly like an explicit choice.
     */
    private var lastAppliedLooping: Boolean? = null

    override suspend fun play(itemId: PlaybackItemId) {
        val engine = enginePort.state.value
        if (itemOf(engine.activeSource) == itemId && engine.phase != PlaybackPhase.Failed) {
            // The engine is already holding this item's source; there is nothing to resolve.
            intent.update { it.superseded() }
            enginePort.submit(PlaybackCommand.Play)
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
                    enginePort.submit(PlaybackCommand.Load(loadRequest(itemId, resolution)))
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
        enginePort.submit(PlaybackCommand.Pause)
    }

    override fun setLooping(enabled: Boolean) {
        loopPreferenceEstablished = true
        enginePort.submit(PlaybackCommand.SetLooping(enabled))
    }

    override fun setVolume(volume: Float) {
        require(volume.isFinite()) { "Volume must be finite." }
        enginePort.submit(PlaybackCommand.SetVolume(volume.coerceIn(0.0f, 1.0f)))
    }

    override fun startSleepTimer(durationMs: Long) {
        enginePort.submit(PlaybackCommand.StartSleepTimer(durationMs))
    }

    override fun cancelSleepTimer() {
        enginePort.submit(PlaybackCommand.CancelSleepTimer)
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
     * The one place an item id is flattened on the way out: `core:playback` is a standalone
     * library and its [AudioSource] identifies a source by plain string, as it should. The kind
     * travels inside that string, so two items sharing a raw id stay two sources.
     */
    private fun loadRequest(
        itemId: PlaybackItemId,
        resolved: ItemResolution.Resolved,
    ): PlaybackRequest = PlaybackRequest(
        source = AudioSource(itemId.toEngineId(), resolved.uri, resolved.title, resolved.artist),
        autoplay = true,
        loopMode = if (loadLooping(resolved.policy.defaultLooping)) LoopMode.One else LoopMode.Off,
        volume = enginePort.state.value.volume,
    )

    /**
     * What looping should be for the item about to replace whatever the engine currently holds.
     *
     * [defaultLooping] wins unless [loopPreferenceEstablished] says the listener chose explicitly,
     * or the engine's current `isLooping` has drifted from [lastAppliedLooping] — the value this
     * adapter itself set for the outgoing item, which a drift means a remote controller changed
     * since. Comparing against that recorded value, rather than reusing [effectiveLooping]'s cruder
     * "something is attached" check, is what keeps a switch between kinds from inheriting whatever
     * loop value the outgoing item's own default happened to leave behind: without it, a sound
     * playing first made every story after it loop, and a story playing first made every sound
     * after it not loop, with no preference — local or remote — ever established.
     */
    private fun loadLooping(defaultLooping: Boolean): Boolean {
        val currentLooping = enginePort.state.value.isLooping
        val looping = when {
            loopPreferenceEstablished -> currentLooping
            lastAppliedLooping != null && currentLooping != lastAppliedLooping -> {
                // Nothing this adapter did changed it since the last load — a remote controller did.
                loopPreferenceEstablished = true
                currentLooping
            }
            else -> defaultLooping
        }
        lastAppliedLooping = looping
        return looping
    }

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
     * What the summary should show as the current loop state, before or after anything has loaded.
     *
     * Called with no argument from [summaryOf]. Once anything is attached or requested, the screen
     * shows the engine's real, reconciled [AudioPlayerState.isLooping] instead of a re-derived
     * default — [AudioPlayerState.activeSource], not `source`, is the test for that: a dropped
     * service connection clears only the *attached* source while the session's reconciled settings
     * live on, so keying off `source` would show a listener's choice reverting mid-reconnect.
     *
     * Deciding a *new* item's own loop default is a different question, answered by [loadLooping]:
     * a previous item merely being attached is not a listener preference, and must not leak into
     * whatever plays next.
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
