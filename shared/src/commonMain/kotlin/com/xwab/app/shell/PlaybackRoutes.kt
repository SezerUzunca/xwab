package com.xwab.app.shell

import androidx.navigation3.runtime.NavKey
import com.xwab.app.core.guide.port.GUIDE_PLAYBACK_KIND
import com.xwab.app.core.rehearsal.port.REHEARSAL_PLAYBACK_KIND
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.shuffle.port.SHUFFLE_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.feature.aftershift.navigation.AfterShiftRoute
import com.xwab.app.feature.browse.navigation.BrowseRoute
import com.xwab.app.feature.rehearsal.navigation.RehearsalRoute
import com.xwab.app.feature.shuffle.navigation.ShuffleRoute
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.feature.story.navigation.StoryRoute

/**
 * Opens a playing item's own screen, in the tab that item belongs to: a sound under Sounds, a
 * story under Stories, a word deck on the Shuffle tab's one screen, a guide on After Shift's and
 * the listener's rewritten nightmare on its screen under After Shift.
 * [open] receives that tab and the item's route.
 */
internal fun openPlaybackDetails(item: PlaybackItemId, open: (tab: NavKey, route: NavKey) -> Unit) {
    when (item.kind) {
        SOUND_PLAYBACK_KIND -> open(BrowseRoute, SoundRoute(item.value))
        STORY_PLAYBACK_KIND -> open(StoriesRoute, StoryRoute(item.value))
        // Every deck plays from the tab's root, so that is the deck's own screen; a guide likewise.
        SHUFFLE_PLAYBACK_KIND -> open(ShuffleRoute, ShuffleRoute)
        GUIDE_PLAYBACK_KIND -> open(AfterShiftRoute, AfterShiftRoute)
        // There is one script, so its screen takes no id.
        REHEARSAL_PLAYBACK_KIND -> open(AfterShiftRoute, RehearsalRoute)
        else -> return
    }
}

/**
 * The item a destination is the own screen of, or null for a screen about no single item.
 *
 * The reverse of [openPlaybackDetails]: while a sound's or story's detail is showing, the
 * now-playing bar for that same item would repeat the detail's play/pause and timer. The Shuffle and
 * After Shift screens name none: each is about all of its items, as a list is, and keeps the bar as
 * lists do.
 */
internal fun NavKey.playbackItem(): PlaybackItemId? = when (this) {
    is SoundRoute -> PlaybackItemId(SOUND_PLAYBACK_KIND, trackId)
    is StoryRoute -> PlaybackItemId(STORY_PLAYBACK_KIND, storyId)
    else -> null
}
