package com.xwab.app.composition

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute

/**
 * Opens a playing item's own screen, in the tab that item belongs to: a sound under Sounds, a
 * story under Stories. [open] receives that tab and the item's route.
 */
internal fun openPlaybackDetails(item: PlaybackItemId, open: (tab: NavKey, route: NavKey) -> Unit) {
    when (item.kind) {
        SOUND_PLAYBACK_KIND -> open(BrowseRoute, SoundRoute(item.value))
        STORY_PLAYBACK_KIND -> open(StoriesRoute, StoryRoute(item.value))
        else -> return
    }
}

/**
 * The item a destination is the own screen of, or null for a screen about no single item.
 *
 * The reverse of [openPlaybackDetails]: while a sound's or story's detail is showing, the
 * now-playing bar for that same item would repeat the detail's play/pause and timer.
 */
internal fun NavKey.playbackItem(): PlaybackItemId? = when (this) {
    is SoundRoute -> PlaybackItemId(SOUND_PLAYBACK_KIND, trackId)
    is StoryRoute -> PlaybackItemId(STORY_PLAYBACK_KIND, storyId)
    else -> null
}
