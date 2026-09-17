@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.xwab.app.composition

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.xwab.app.di.AppGraph
import com.xwab.app.feature.nowplaying.navigation.NowPlayingBar

/** One bar, one key, however many scenes it is carried through. */
private const val NOW_PLAYING_BAR_KEY = "now-playing-bar"

/**
 * The now-playing bar, drawn inside the navigation area rather than beside it.
 *
 * `Scaffold`'s `bottomBar` would be simpler and is what Google's Common UI recipe uses. The bar is
 * here instead because a scene decorator is the only place it can reach `NavDisplay`'s
 * [SharedTransitionScope]: a bar that expands into the screen for what it is playing has to be able
 * to hand its title and artwork to that screen, and shared elements only match inside one
 * `SharedTransitionLayout`. Nothing uses that yet — the bar is not tappable — so today this is the
 * same picture in a different place.
 *
 * Follows Google's `navscenedecorator` recipe, including the parts that look odd out of context.
 * See [cacheSize] and the caller election below.
 */
private class NowPlayingScene<T : Any>(
    private val scene: Scene<T>,
    private val sharedTransitionScope: SharedTransitionScope,
    private val bar: @Composable () -> Unit,
) : Scene<T> by scene {

    /** Distinct from the scene it wraps, so `NavDisplay` animates between decorated scenes. */
    override val key = scene::class to scene.key

    override val content = @Composable {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        // Both scenes are composed for the length of a transition, and one movable content can
        // only be called from one place. The scene being navigated *to* is the caller; the one
        // leaving holds its space through `cacheSize` and draws nothing.
        val isMovableContentCaller =
            animatedContentScope.transition.targetState == EnterExitState.Visible

        with(sharedTransitionScope) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { scene.content() }
                Box(
                    modifier = Modifier
                        .cacheSize(!isMovableContentCaller)
                        // What keeps the bar still while the screen above it animates: matched by
                        // key across both scenes, so it is moved rather than cross-faded.
                        .sharedElement(
                            rememberSharedContentState(NOW_PLAYING_BAR_KEY),
                            animatedContentScope,
                        ),
                ) {
                    if (isMovableContentCaller) bar()
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
 * The bar is wrapped in `movableContentOf` here rather than inside the scene: that is what lets the
 * same composition — and everything it remembers — be carried from the outgoing scene to the
 * incoming one instead of being built again.
 */
@Composable
internal fun <T : Any> rememberNowPlayingSceneDecoratorStrategy(
    graph: AppGraph,
    sharedTransitionScope: SharedTransitionScope,
): NowPlayingSceneDecoratorStrategy<T> {
    val bar: @Composable () -> Unit = { NowPlayingBar(graph.nowPlayingDependencies) }
    val currentBar by rememberUpdatedState(bar)
    val movableBar = remember { movableContentOf { currentBar() } }
    return remember(sharedTransitionScope) {
        NowPlayingSceneDecoratorStrategy(sharedTransitionScope, movableBar)
    }
}
