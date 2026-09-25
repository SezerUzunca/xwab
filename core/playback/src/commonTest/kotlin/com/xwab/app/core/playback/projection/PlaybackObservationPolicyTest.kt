package com.xwab.app.core.playback.projection

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackObservationPolicyTest {

    /** A ready item, paused or playing, with nothing outstanding. Each test changes what it needs. */
    private val settled = EngineTransitionState(
        hasCurrentItem = true,
        hasFailure = false,
        isReadyToPlay = true,
        isWaitingToPlay = false,
        playTransitionTicksRemaining = 0,
        awaitingReadiness = false,
    )

    private fun shouldObserve(state: EngineTransitionState) = shouldObserveEngineTransition(state)

    @Test
    fun anItemThatIsNotReadyYetIsWatched() {
        assertTrue(shouldObserve(settled.copy(isReadyToPlay = false)))
    }

    @Test
    fun aPlayerWaitingToPlayIsWatched() {
        assertTrue(shouldObserve(settled.copy(isWaitingToPlay = true)))
    }

    @Test
    fun aSettledPlayerIsNotWatched() {
        assertFalse(shouldObserve(settled))
    }

    @Test
    fun aSettledPlayerIsStillWatchedWhileAPlayCommandIsCatchingUp() {
        assertTrue(shouldObserve(settled.copy(playTransitionTicksRemaining = 1)))
        assertFalse(shouldObserve(settled.copy(playTransitionTicksRemaining = 0)))
    }

    @Test
    fun anEmptyOrFailedPlayerIsNotWatchedAtAll() {
        assertFalse(shouldObserve(settled.copy(hasCurrentItem = false, isReadyToPlay = false)))
        assertFalse(shouldObserve(settled.copy(hasFailure = true, isReadyToPlay = false)))
        // Not even for the remainder of a play transition: a failure is terminal for the
        // engine, and the facade suspends observation on top of this.
        assertFalse(shouldObserve(settled.copy(hasFailure = true, playTransitionTicksRemaining = 5)))
    }

    @Test
    fun anEmptyPlayerOutranksAPendingPlayTransition() {
        assertFalse(shouldObserve(settled.copy(hasCurrentItem = false, playTransitionTicksRemaining = 5)))
    }

    @Test
    fun aLoadAwaitingReadinessIsWatchedEvenOnceTheQueueEmpties() {
        // A queue player drops an item that fails to open; nothing else would report it.
        assertTrue(
            shouldObserve(
                settled.copy(hasCurrentItem = false, isReadyToPlay = false, awaitingReadiness = true),
            ),
        )
    }

    @Test
    fun aFailureEndsTheWaitForReadiness() {
        assertFalse(
            shouldObserve(
                settled.copy(hasFailure = true, isReadyToPlay = false, awaitingReadiness = true),
            ),
        )
    }
}
