package com.xwab.app.core.media

internal enum class NowPlayingUpdateAction {
    None,
    Clear,
    Publish,
}

internal data class NowPlayingPublicationKey(
    val sourceId: String,
    val phase: PlaybackPhase,
    val isPlaying: Boolean,
)

internal fun decideNowPlayingUpdate(
    previous: NowPlayingPublicationKey?,
    source: AudioSource?,
    phase: PlaybackPhase,
    isPlaying: Boolean,
    force: Boolean,
): NowPlayingUpdateAction {
    if (source == null || phase == PlaybackPhase.Failed) return NowPlayingUpdateAction.Clear
    val next = NowPlayingPublicationKey(source.id, phase, isPlaying)
    return if (force || next != previous) {
        NowPlayingUpdateAction.Publish
    } else {
        NowPlayingUpdateAction.None
    }
}
