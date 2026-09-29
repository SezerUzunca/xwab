package com.xwab.app.composition

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoryRoute

/** Removes the player before opening details, so Back returns to the screen that opened it. */
internal fun openPlaybackDetails(item: PlaybackItemId, onReplace: (NavKey) -> Unit) {
    val route = when (item.kind) {
        SOUND_PLAYBACK_KIND -> SoundRoute(item.value)
        STORY_PLAYBACK_KIND -> StoryRoute(item.value)
        else -> return
    }
    onReplace(route)
}

/**
 * The item a destination is the own screen of, or null for a screen about no single item.
 *
 * The reverse of [openPlaybackDetails]: while a sound's or story's detail is showing, the mini
 * player for that same item would repeat the detail's play/pause and timer.
 */
internal fun NavKey.playbackItem(): PlaybackItemId? = when (this) {
    is SoundRoute -> PlaybackItemId(SOUND_PLAYBACK_KIND, trackId)
    is StoryRoute -> PlaybackItemId(STORY_PLAYBACK_KIND, storyId)
    else -> null
}
