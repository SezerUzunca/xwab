package com.xwab.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.Scene

private const val TRANSITION_MILLIS = 240

/** Tab changes have no parent/child direction; entry navigation follows the layout direction. */
internal fun AnimatedContentTransitionScope<out Scene<*>>.navigationTransition(
    forward: Boolean,
    direction: Int,
): ContentTransform {
    if (initialState.entries.last().metadata[TabKey] != targetState.entries.last().metadata[TabKey]) {
        return fadeIn(tween(TRANSITION_MILLIS)) togetherWith fadeOut(tween(TRANSITION_MILLIS))
    }
    return horizontalTransition(if (forward) direction else -direction)
}

private fun horizontalTransition(direction: Int): ContentTransform =
    slideInHorizontally(tween(TRANSITION_MILLIS), initialOffsetX = { it * direction }) togetherWith
        slideOutHorizontally(tween(TRANSITION_MILLIS), targetOffsetX = { -it * direction })
