@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.xwab.app.composition

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.xwab.app.core.session.port.PlaybackItemId
import com.xwab.app.core.sound.port.SOUND_PLAYBACK_KIND
import com.xwab.app.core.story.port.STORY_PLAYBACK_KIND
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.nowplaying.navigation.NowPlayingBar
import com.xwab.app.feature.sound.navigation.SoundRoute
import com.xwab.app.feature.story.navigation.StoriesRoute

/** One bar, one key, however many scenes carry it. */
private const val NOW_PLAYING_BAR_KEY = "now-playing-bar"

/**
 * The now-playing bar, drawn inside the navigation area rather than beside it.
 *
 * `Scaffold`'s `bottomBar` would be simpler and is where the tab bar still lives. The bar is here
 * instead because a scene decorator is the only place chrome can reach `NavDisplay`'s
 * [SharedTransitionScope]: a bar that expands into the screen for what it is playing has to hand
 * its content to that screen, and shared elements only match inside one `SharedTransitionLayout`.
 * The bar now opens what it is holding; the expand-into-the-screen animation is what the matched
 * element below is still groundwork for.
 *
 * `NavDisplay` animates between *decorated* scenes, so for the length of every navigation the
 * outgoing and the incoming scene are both composed and both draw a bar. [NOW_PLAYING_BAR_KEY]
 * matches the two, which is what moves the bar instead of cross-fading it — without it a listener
 * would see it dim at every navigation.
 *
 * Google's `navscenedecorator` recipe does more than this: it carries one `movableContentOf`
 * between the scenes and holds the vacated space with a size-caching modifier. That machinery
 * exists because its navigation bar owns animation state that must not be duplicated. This bar owns
 * none — its state is a ViewModel on the root store, so both compositions read the same instance
 * and the same values — so the plain matched pair is enough. Give the bar state of its own and the
 * recipe's version becomes the right one again.
 */
private data class NowPlayingScene<T : Any>(
    private val scene: Scene<T>,
    private val sharedTransitionScope: SharedTransitionScope,
    private val bar: @Composable () -> Unit,
) : Scene<T> by scene {

    /** Distinct from the scene it wraps, so `NavDisplay` animates between decorated scenes. */
    override val key = scene::class to scene.key

    override val content = @Composable {
        with(sharedTransitionScope) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { scene.content() }
                Box(
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(NOW_PLAYING_BAR_KEY),
                        LocalNavAnimatedContentScope.current,
                    ),
                ) {
                    bar()
                }
            }
        }
    }
}

internal class NowPlayingSceneDecoratorStrategy<T : Any>(
    private val sharedTransitionScope: SharedTransitionScope,
    private val bar: @Composable () -> Unit,
) : SceneDecoratorStrategy<T> {
    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> =
        NowPlayingScene(scene, sharedTransitionScope, bar)
}

/**
 * Where this feature is connected to the app shell, the way [appEntryProvider] connects the
 * features that are destinations. The composition root is the only module allowed to name either.
 *
 * The bar lambda is remembered rather than rebuilt: it is what [NowPlayingScene] compares itself on.
 */
@Composable
internal fun <T : Any> rememberNowPlayingSceneDecoratorStrategy(
    graph: AppGraph,
    sharedTransitionScope: SharedTransitionScope,
    onNavigate: (NavKey) -> Unit,
): NowPlayingSceneDecoratorStrategy<T> {
    val bar: @Composable () -> Unit = remember(graph, onNavigate) {
        {
            NowPlayingBar(
                graph.nowPlayingDependencies,
                onOpen = { item -> item.route()?.let(onNavigate) },
            )
        }
    }
    return remember(sharedTransitionScope, bar) {
        NowPlayingSceneDecoratorStrategy(sharedTransitionScope, bar)
    }
}

/**
 * Where a playing item is on screen — the app's answer, not the bar's.
 *
 * A sound has a screen of its own. A story does not: every story is played from its row, so the
 * nearest thing to "where this came from" is the list it is in. Both are decisions only the module
 * that owns the routes can make, which is why the bar hands over an id and nothing else.
 */
internal fun PlaybackItemId.route(): NavKey? = when (kind) {
    SOUND_PLAYBACK_KIND -> SoundRoute(value)
    STORY_PLAYBACK_KIND -> StoriesRoute
    // A kind this build has no content module for. It can still reach here from a playback service
    // that outlived an older build, and there is nothing to open for it.
    else -> null
}
