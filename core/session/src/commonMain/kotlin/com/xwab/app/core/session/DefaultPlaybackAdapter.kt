// The one consumer of the resolver contract: this module owns it, and the session is what looks
// resolvers up. Content modules opt in to implement it; screens never do.
@file:OptIn(PlaybackResolverApi::class)

package com.xwab.app.core.session

import com.xwab.app.core.session.port.DEFAULT_LOOPING
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackPort
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.core.session.port.VOLUME_RANGE
import com.xwab.app.core.session.port.ItemResolution
import com.xwab.app.core.session.port.PlaybackItemResolver
import com.xwab.app.core.session.port.PlaybackResolverApi
import com.xwab.app.core.playback.port.AudioPlayerState
import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.LoopMode
import com.xwab.app.core.playback.port.PlaybackCommand
import com.xwab.app.core.playback.port.PlaybackEnginePort
import com.xwab.app.core.playback.port.PlaybackErrorCode
import com.xwab.app.core.playback.port.PlaybackPhase
import com.xwab.app.core.playback.port.PlaybackRequest
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
internal class DefaultPlaybackAdapter
@Inject
internal constructor(
    private val enginePort: PlaybackEnginePort,
    /**
     * One resolver per content kind, keyed by the kind each module registered itself under.
     *
     * Injected as a multibinding rather than built here, which is the whole of this module's
     * independence from content: a new content type contributes its own entry from its own module
     * and this constructor never changes. Metro aggregates the map from the compile classpath, so
     * the map holds exactly the content modules the composition root declares — a removed one is
     * simply absent, and the kind it used to answer for reports `ItemNotFound`.
     * With no contributions Metro uses the optional empty map, so the session still exists after
     * the last content module is removed.
     *
     * Keys cannot collide: a map is a map, and two modules registering the same kind is a Metro
     * duplicate-binding error at compile time rather than one resolver silently never running.
     */
    private val resolversByKind: Map<String, PlaybackItemResolver> = emptyMap(),
) : PlaybackPort {

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
    private val loopPreferenceEstablished: Boolean get() = intent.value.loopPreferenceEstablished

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
        val resolver = resolversByKind[itemId.kind]
        val engine = enginePort.state.value
        if (
            resolver != null &&
            itemOf(engine.activeSource) == itemId &&
            engine.phase != PlaybackPhase.Failed
        ) {
            // The engine is already holding this item's source; there is nothing to resolve.
            // A retained engine source cannot restore a content capability removed from this build.
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
            if (resolver == null) return settle(generation, PlaybackFailure.ItemNotFound(itemId))

            when (val resolution = resolver.resolve(itemId.value)) {
                is ItemResolution.Resolved -> {
                    // A newer play() or a pause() arrived while the lookup was running; its own
                    // state is the current one, and loading now would undo what was last asked for.
                    if (intent.value.generation != generation) return
                    enginePort.submit(PlaybackCommand.Load(loadRequest(itemId, resolution)))
                    settle(generation, failure = null, named = itemId to resolution.displayName)
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
        enginePort.submit(PlaybackCommand.SetLooping(enabled))
        intent.update { it.copy(loopPreferenceEstablished = true) }
    }

    override fun setVolume(volume: Float) {
        require(volume.isFinite()) { "Volume must be finite." }
        enginePort.submit(PlaybackCommand.SetVolume(volume.coerceIn(VOLUME_RANGE)))
    }

    override fun startSleepTimer(durationMs: Long) {
        enginePort.submit(PlaybackCommand.StartSleepTimer(durationMs))
    }

    override fun cancelSleepTimer() {
        enginePort.submit(PlaybackCommand.CancelSleepTimer)
    }

    /**
     * Releases the pending claim and records the outcome, unless a newer request has taken over.
     *
     * @param named the item that was just resolved and the name this app lists it under, when the
     *   outcome is a successful resolution. Null on every failing path, which leaves whatever name
     *   was already held: a failed lookup falls back to the item that is still loaded, and that
     *   item's name is still the right one to show.
     */
    private fun settle(
        generation: Long,
        failure: PlaybackFailure?,
        named: Pair<PlaybackItemId, String>? = null,
    ) {
        intent.update {
            if (it.generation != generation) return@update it
            it.copy(
                pendingItemId = null,
                failure = failure,
                namedItemId = named?.first ?: it.namedItemId,
                displayName = named?.second ?: it.displayName,
            )
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
        // Clamped for the same reason the published summary is, but with a sharper edge: a
        // `PlaybackRequest` *refuses* a volume outside the range, so an engine reporting its own
        // idea of loudness would not produce a wrong number here — it would throw, and take the
        // next load with it. The engine is only ever sent values inside the range, so this catches
        // nothing today; what it removes is a load that fails for a reason no listener caused.
        volume = enginePort.state.value.volume.coerceIn(VOLUME_RANGE),
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
                intent.update { it.copy(loopPreferenceEstablished = true) }
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
        val held = itemOf(engine.activeSource)
        val requested = wanted.pendingItemId ?: held
        val active = itemOf(engine.source)
        val playIntent = wanted.pendingItemId != null || engine.playRequested

        return PlaybackSummary(
            requestedItemId = requested,
            activeItemId = active,
            playIntent = playIntent,
            // The name this app lists the item under, which is not the one it hands the platform:
            // a screen showing "Gentle Rain" two rows under a list that says "Rain on the Window"
            // is naming the same sound twice. The engine's title is the fallback rather than the
            // answer — it is all that survives a reconnect to a service that outlived the process,
            // and a notification's name beats no name. Both are gated on the engine actually
            // holding what was asked for; mid-switch neither belongs to the incoming item.
            title = wanted.nameOf(requested)
                ?: engine.activeSource?.title?.takeIf { held == requested },
            isPlaying = engine.isPlaying,
            // About the *requested* item: a different sound being audible does not make the one
            // that was asked for ready.
            isPreparing = playIntent &&
                (requested != active || !engine.isPlaying) &&
                engine.phase != PlaybackPhase.Failed,
            isLooping = engine.effectiveLooping(wanted.loopPreferenceEstablished),
            // Clamped on the way out as well as in. Everything this adapter sends the engine is
            // already inside the range, so this only catches an engine reporting its own idea of
            // loudness — but the published range is a promise to every reader, and a promise kept
            // only while the layer below behaves is not one a screen can build on.
            volume = engine.volume.coerceIn(VOLUME_RANGE),
            failure = wanted.failure ?: engine.engineFailure(),
        )
    }

    /** The item an engine source names, or null when the id names nothing this session can act on. */
    private fun itemOf(source: AudioSource?): PlaybackItemId? =
        source?.id?.let { playbackItemIdOf(it) }

    /**
     * What the summary should show as the current loop state, before or after anything has loaded.
     *
     * Uses the same session snapshot as [summaryOf]. Once anything is attached or requested, the screen
     * shows the engine's real, reconciled [AudioPlayerState.isLooping] instead of a re-derived
     * default — [AudioPlayerState.activeSource], not `source`, is the test for that: a dropped
     * service connection clears only the *attached* source while the session's reconciled settings
     * live on, so keying off `source` would show a listener's choice reverting mid-reconnect.
     *
     * Deciding a *new* item's own loop default is a different question, answered by [loadLooping]:
     * a previous item merely being attached is not a listener preference, and must not leak into
     * whatever plays next.
     */
    private fun AudioPlayerState.effectiveLooping(preferenceEstablished: Boolean): Boolean =
        if (preferenceEstablished || activeSource != null) isLooping else DEFAULT_LOOPING

    /**
     * Reads the engine's own verdict instead of flattening every failure into one.
     *
     * The engine already distinguishes the two cases this session publishes: a source it was handed
     * and could never open ([PlaybackErrorCode.InvalidSource]) or never got ready
     * ([PlaybackErrorCode.Timeout]) is a source that could not be reached, which is worth another
     * tap; a source it had accepted and then failed on is not. Until this, both arrived as
     * [PlaybackFailure.EngineFailed], so a listener with no network was told the item could not be
     * played rather than that it could not be reached — the one answer that would have told them
     * trying again might work.
     */
    private fun AudioPlayerState.engineFailure(): PlaybackFailure? =
        if (phase == PlaybackPhase.Failed) {
            itemOf(activeSource)?.let { item ->
                when (error?.code) {
                    PlaybackErrorCode.InvalidSource,
                    PlaybackErrorCode.Timeout,
                    -> PlaybackFailure.SourceUnavailable(item)
                    else -> PlaybackFailure.EngineFailed(item)
                }
            }
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
        val loopPreferenceEstablished: Boolean = false,
        val generation: Long = 0L,
        val pendingItemId: PlaybackItemId? = null,
        val failure: PlaybackFailure? = null,
        /** The item [displayName] belongs to, so a stale name cannot be shown against a new one. */
        val namedItemId: PlaybackItemId? = null,
        val displayName: String? = null,
    ) {
        /**
         * A new listener action: whatever was in flight no longer counts, and neither does a
         * failure. The name survives — it belongs to whatever is loaded, not to the request that
         * was abandoned, and pausing does not rename what is paused.
         */
        fun superseded(): SessionIntent =
            copy(generation = generation + 1, pendingItemId = null, failure = null)

        /** The name held for [itemId], or null when the one held is for something else. */
        fun nameOf(itemId: PlaybackItemId?): String? =
            displayName?.takeIf { itemId != null && itemId == namedItemId }
    }
}
