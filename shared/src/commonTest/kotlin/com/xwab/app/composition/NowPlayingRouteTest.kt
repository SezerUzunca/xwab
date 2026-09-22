package com.xwab.app.composition

import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute
import com.xwab.app.navigation.TOP_LEVEL_DESTINATIONS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Where the now-playing bar leads.
 *
 * The bar hands over a [PlaybackItemId] and knows nothing else; this module turns it into a route.
 * Worth checking from the outside because the two kinds are answered differently for a reason, and
 * a mapping that quietly sent a story to a sound screen would compile.
 */
class NowPlayingRouteTest {

    @Test
    fun aPlayingSoundOpensItsOwnScreen() {
        assertEquals(
            SoundRoute("gentle-rain"),
            PlaybackItemId(SOUND_PLAYBACK_KIND, "gentle-rain").route(),
        )
    }

    /**
     * A story has no screen of its own — it is played from its row — so the nearest thing to where
     * it came from is the list. That list is a tab, which is what makes this worth stating: the
     * navigator switches tabs rather than pushing, so the bar cannot bury Stories on top of
     * another tab's history.
     */
    @Test
    fun aPlayingStoryOpensTheListItIsPlayedFrom() {
        val route = PlaybackItemId(STORY_PLAYBACK_KIND, "moonlit-forest").route()

        assertEquals(StoriesRoute, route)
        assertTrue(
            TOP_LEVEL_DESTINATIONS.any { it.route == route },
            "the stories list is a tab, so opening it must go through the tab rules",
        )
    }
}
