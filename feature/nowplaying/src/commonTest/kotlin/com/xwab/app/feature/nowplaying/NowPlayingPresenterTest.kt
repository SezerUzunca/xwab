package com.xwab.app.feature.nowplaying

import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.session.port.PlaybackSummary
import com.xwab.app.testing.FakePlaybackPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class NowPlayingPresenterTest {

    @Test
    fun aSessionThatHasNeverBeenAskedForAnythingShowsNothing() = runTest {
        assertTrue(presenter(PlaybackSummary()).state.first().idle)
    }

    @Test
    fun theBarNamesWhatTheSessionIsOn() = runTest {
        val state = presenter(
            PlaybackSummary(requestedItemId = RAIN, title = "Gentle Rain", playIntent = true),
        ).state.first()

        assertFalse(state.idle)
        assertEquals("Gentle Rain", state.title)
        assertTrue(state.playIntent)
    }

    /**
     * The session withholds a title for exactly as long as it is switching, so the bar has a name
     * for the incoming item or no name at all — never the outgoing one's.
     */
    @Test
    fun aSwitchIsShownWithoutANameUntilTheSessionHasOne() = runTest {
        val state = presenter(
            PlaybackSummary(
                requestedItemId = WAVES,
                title = null,
                playIntent = true,
                isPreparing = true,
            ),
        ).state.first()

        assertFalse(state.idle, "the bar stays up across a switch")
        assertNull(state.title)
        assertTrue(state.isPreparing)
    }

    @Test
    fun aTapPausesWhatTheControlShowsAsPlaying() = runTest {
        val port = FakePlaybackPort()
        val presenter = NowPlayingPresenter(port)
        val playing = NowPlayingState(itemId = RAIN, playIntent = true)

        presenter.togglePlayback(playing)

        assertEquals(1, port.pauses)
        assertNull(port.playedItemId)
    }

    @Test
    fun aTapResumesWhatTheControlShowsAsStopped() = runTest {
        val port = FakePlaybackPort()
        val presenter = NowPlayingPresenter(port)
        val stopped = NowPlayingState(itemId = RAIN, playIntent = false)

        presenter.togglePlayback(stopped)

        assertEquals(RAIN, port.playedItemId)
        assertEquals(0, port.pauses)
    }

    /**
     * The bar draws nothing when the session is idle, so this can only be reached by a tap racing
     * the state it was drawn from. It must not resolve to a play with no item.
     */
    @Test
    fun aTapWithNothingToActOnDoesNothing() = runTest {
        val port = FakePlaybackPort()
        val presenter = NowPlayingPresenter(port)

        presenter.togglePlayback(NowPlayingState())

        assertNull(port.playedItemId)
        assertEquals(0, port.pauses)
    }

    private fun presenter(summary: PlaybackSummary): NowPlayingPresenter =
        NowPlayingPresenter(FakePlaybackPort().apply { publish(summary) })

    private companion object {
        val RAIN = PlaybackItemId.sound("gentle-rain")
        val WAVES = PlaybackItemId.sound("calm-waves")
    }
}
