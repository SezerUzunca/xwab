package com.xwab.app.composition

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * Holds the last measured size while its content is absent.
 *
 * Taken from Google's `navscenedecorator` recipe, which is where this whole arrangement comes from.
 * It exists because of how [rememberNowPlayingSceneDecoratorStrategy] shares one bar between two
 * scenes: during a transition both the outgoing and the incoming scene are composed, but only one
 * of them may call the movable content. The scene that does not call it still has to occupy the
 * same space, or the screen above it would stretch for the length of every navigation.
 */
internal fun Modifier.cacheSize(useCachedSize: Boolean): Modifier =
    this.then(CacheSizeElement(useCachedSize))

private data class CacheSizeElement(val useCachedSize: Boolean) :
    ModifierNodeElement<CacheSizeNode>() {
    override fun create() = CacheSizeNode(useCachedSize)

    override fun update(node: CacheSizeNode) {
        node.useCachedSize = useCachedSize
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cacheSize"
        properties["useCachedSize"] = useCachedSize
    }
}

private class CacheSizeNode(useCachedSize: Boolean) : Modifier.Node(), LayoutModifierNode {
    var useCachedSize: Boolean = useCachedSize
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasurement()
            }
        }

    private var isSizeCached = false
    private var cachedSize: IntSize = IntSize.Zero

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val currentSize = IntSize(placeable.width, placeable.height)
        val size = if (useCachedSize && isSizeCached) cachedSize else currentSize
        cachedSize = size
        isSizeCached = true
        return layout(size.width, size.height) { placeable.placeRelative(0, 0) }
    }
}
