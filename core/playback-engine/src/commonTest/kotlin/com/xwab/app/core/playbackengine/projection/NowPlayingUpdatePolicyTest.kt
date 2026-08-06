package com.xwab.app.core.playbackengine.projection

import com.xwab.app.core.playbackengine.api.AudioSource
import com.xwab.app.core.playbackengine.api.PlaybackPhase
import kotlin.test.Test
import kotlin.test.assertEquals

class NowPlayingUpdatePolicyTest {
    private val source = AudioSource(id = "rain", uri = "file:///rain.mp3")
    private val readyPaused = NowPlayingPublicationKey(
        sourceId = source.id,
        phase = PlaybackPhase.Ready,
        isPlaying = false,
    )

    @Test
    fun nullSourceAndFailureClearNowPlaying() {
        assertEquals(
            NowPlayingUpdateAction.Clear,
            decideNowPlayingUpdate(readyPaused, null, PlaybackPhase.Idle, false, false),
        )
        assertEquals(
            NowPlayingUpdateAction.Clear,
            decideNowPlayingUpdate(readyPaused, source, PlaybackPhase.Failed, false, false),
        )
    }

    @Test
    fun unchangedSnapshotIsNotRepublishedUnlessForced() {
        assertEquals(
            NowPlayingUpdateAction.None,
            decideNowPlayingUpdate(readyPaused, source, PlaybackPhase.Ready, false, false),
        )
        assertEquals(
            NowPlayingUpdateAction.Publish,
            decideNowPlayingUpdate(readyPaused, source, PlaybackPhase.Ready, false, true),
        )
    }

    @Test
    fun sourcePhaseAndPlaybackChangesArePublished() {
        assertEquals(
            NowPlayingUpdateAction.Publish,
            decideNowPlayingUpdate(readyPaused, source.copy(id = "ocean"), PlaybackPhase.Ready, false, false),
        )
        assertEquals(
            NowPlayingUpdateAction.Publish,
            decideNowPlayingUpdate(readyPaused, source, PlaybackPhase.Buffering, false, false),
        )
        assertEquals(
            NowPlayingUpdateAction.Publish,
            decideNowPlayingUpdate(readyPaused, source, PlaybackPhase.Ready, true, false),
        )
    }
}
