package com.xwab.app.core.playback.store

import com.xwab.app.core.playback.port.AudioSource
import com.xwab.app.core.playback.port.PlaybackRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for the common [PlaybackStore]: the mailbox drain, its
 * re-entrancy guard, effect ordering, and one publication per message. What a
 * message does to the state is [reducePlayback]'s contract, tested there.
 */
class PlaybackStoreTest {

    private val sourceA = AudioSource(id = "rain", uri = "file:///rain.mp3")

    /** Records executed effects and can feed engine events back into the store. */
    private class RecordingDriver {
        lateinit var store: PlaybackStore
        val effects = mutableListOf<PlaybackSideEffect>()
        val publishedStates = mutableListOf<PlaybackState>()
        var onEffect: (PlaybackSideEffect) -> Unit = {}

        fun executeEffects(effects: List<PlaybackSideEffect>) {
            this.effects += effects
            effects.forEach(onEffect)
        }

        fun onStateChanged() {
            publishedStates += store.state
        }
    }

    private fun newStore(): Pair<PlaybackStore, RecordingDriver> {
        val driver = RecordingDriver()
        val store = PlaybackStore(driver::executeEffects, driver::onStateChanged)
        driver.store = store
        return store to driver
    }

    @Test
    fun reentrantEngineLoadedIsQueuedAndAutoPlaysInOrder() {
        val (store, driver) = newStore()
        // While executing LoadSource, the driver reports the source loaded. That message
        // re-enters dispatch and must be queued and processed AFTER the current one.
        driver.onEffect = { effect ->
            if (effect is PlaybackSideEffect.LoadSource) {
                store.dispatch(
                    PlaybackMessage.EngineSourceLoaded(effect.operationId, effect.request.source),
                )
            }
        }

        store.dispatch(PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = true)))

        assertEquals(
            listOf(PlaybackSideEffect.LoadSource::class, PlaybackSideEffect.Play::class),
            driver.effects.map { it::class },
        )
        assertEquals(sourceA, store.state.observed.source)
        assertTrue(store.state.desired.playRequested)
    }

    @Test
    fun statePublishedAfterEveryProcessedMessage() {
        val (store, driver) = newStore()

        store.dispatch(PlaybackMessage.Load(PlaybackRequest(sourceA, autoplay = false)))
        store.dispatch(PlaybackMessage.SetVolume(0.5f))

        // Two dispatched messages, no re-entrancy => exactly two publications.
        assertEquals(2, driver.publishedStates.size)
        assertEquals(0.5f, store.state.desired.volume)
    }
}
