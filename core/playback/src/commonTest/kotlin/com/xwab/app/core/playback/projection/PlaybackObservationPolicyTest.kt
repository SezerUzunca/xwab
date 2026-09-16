package com.xwab.app.core.playback.projection

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackObservationPolicyTest {

    private fun shouldObserve(
        hasCurrentItem: Boolean = true,
        hasFailure: Boolean = false,
        isReadyToPlay: Boolean = true,
        isWaitingToPlay: Boolean = false,
        playTransitionTicksRemaining: Int = 0,
    ) = shouldObserveEngineTransition(
        hasCurrentItem = hasCurrentItem,
        hasFailure = hasFailure,
        isReadyToPlay = isReadyToPlay,
        isWaitingToPlay = isWaitingToPlay,
        playTransitionTicksRemaining = playTransitionTicksRemaining,
    )

    @Test
    fun anItemThatIsNotReadyYetIsWatched() {
        assertTrue(shouldObserve(isReadyToPlay = false))
    }

    @Test
    fun aPlayerWaitingToPlayIsWatched() {
        assertTrue(shouldObserve(isWaitingToPlay = true))
    }

    @Test
    fun aSettledPlayerIsNotWatched() {
        assertFalse(shouldObserve())
    }

    @Test
    fun aSettledPlayerIsStillWatchedWhileAPlayCommandIsCatchingUp() {
        assertTrue(shouldObserve(playTransitionTicksRemaining = 1))
        assertFalse(shouldObserve(playTransitionTicksRemaining = 0))
    }

    @Test
    fun anEmptyOrFailedPlayerIsNotWatchedAtAll() {
        assertFalse(shouldObserve(hasCurrentItem = false, isReadyToPlay = false))
        assertFalse(shouldObserve(hasFailure = true, isReadyToPlay = false))
        // Not even for the remainder of a play transition: a failure is terminal for the
        // engine, and the facade suspends observation on top of this.
        assertFalse(shouldObserve(hasFailure = true, playTransitionTicksRemaining = 5))
    }

    @Test
    fun anEmptyPlayerOutranksAPendingPlayTransition() {
        assertFalse(shouldObserve(hasCurrentItem = false, playTransitionTicksRemaining = 5))
    }
}
