package com.xwab.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val VISIBLE_WHEEL_ROWS = 3
private const val WHEEL_ROW_HEIGHT_SP = 48
private const val WHEEL_FADE = 0.65f
private const val WHEEL_TILT_DEGREES = 22f
private const val WHEEL_SCALE_REDUCTION = 0.12f
private val PREVIEW_MINUTE_VALUES = listOf(15, 30, 60, 90)

/**
 * Three visible values, with the selected value snapped to the highlighted middle row.
 *
 * @param valueLabel how a value is shown and read aloud, unit included ("30 min"), so the wheel needs
 *   no separate unit caption.
 * @param label the wheel's accessible name; not drawn.
 */
@Composable
internal fun DurationWheel(
    state: LazyListState,
    values: List<Int>,
    label: String,
    modifier: Modifier = Modifier,
    valueLabel: (Int) -> String = Int::toString,
) {
    val selectedIndex by remember(state, values) {
        derivedStateOf { state.centeredWheelIndex().coerceIn(values.indices) }
    }
    val scope = rememberCoroutineScope()
    // Grow with the system font size so the numbers never overlap neighbouring rows.
    val itemHeight = with(LocalDensity.current) { WHEEL_ROW_HEIGHT_SP.sp.toDp() }
        .coerceAtLeast(SleepRelaxTheme.dimens.minimumTouchTarget)
    val selectedLabel = valueLabel(values[selectedIndex])
    val haptics = LocalHapticFeedback.current
    // A tick each time a new value reaches the middle row, as a physical picker clicks — so the
    // choice can be felt in the dark. Not on first composition: opening the card is not a change.
    LaunchedEffect(state, values) {
        snapshotFlow { selectedIndex }.drop(1).collect {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(itemHeight * VISIBLE_WHEEL_ROWS).clearAndSetSemantics {
                contentDescription = label
                stateDescription = selectedLabel
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = selectedIndex.toFloat(),
                    range = 0f..values.lastIndex.toFloat(),
                    steps = (values.size - 2).coerceAtLeast(0),
                )
                setProgress { index ->
                    scope.launch { state.scrollToItem(index.roundToInt().coerceIn(values.indices)) }
                    true
                }
            },
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(SleepRelaxTheme.colors.glassWhiteOverlay, SleepRelaxTheme.shapes.small),
            )
            DurationWheelValues(state, values, valueLabel, itemHeight) { index ->
                scope.launch { state.animateScrollToItem(index) }
            }
        }
    }
}

@Composable
private fun DurationWheelValues(
    state: LazyListState,
    values: List<Int>,
    valueLabel: (Int) -> String,
    itemHeight: Dp,
    onValueClick: (Int) -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = itemHeight),
        flingBehavior = rememberSnapFlingBehavior(state, snapPosition = SnapPosition.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count = values.size, key = { values[it] }) { index ->
            DurationWheelValue(state, index, valueLabel(values[index]), itemHeight) { onValueClick(index) }
        }
    }
}

@Composable
private fun DurationWheelValue(
    state: LazyListState,
    index: Int,
    text: String,
    itemHeight: Dp,
    onClick: () -> Unit,
) {
    val isSelected by remember(state, index) { derivedStateOf { state.centeredWheelIndex() == index } }
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    Box(
        modifier = Modifier.fillMaxWidth().height(itemHeight).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (isSelected) SleepRelaxTheme.colors.accent else SleepRelaxTheme.colors.textSecondary,
            style = SleepRelaxTheme.typography.headlineSmall,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                val layout = state.layoutInfo
                val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
                val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                val distance = if (item == null) 0f else ((item.offset + item.size / 2f - center) / itemHeightPx)
                    .coerceIn(-1f, 1f)
                alpha = 1f - abs(distance) * WHEEL_FADE
                rotationX = -distance * WHEEL_TILT_DEGREES
                scaleX = 1f - abs(distance) * WHEEL_SCALE_REDUCTION
                scaleY = scaleX
            },
        )
    }
}

internal fun LazyListState.centeredWheelIndex(): Int {
    val layout = layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
    return layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index
        ?: firstVisibleItemIndex
}

@Preview(widthDp = 140)
@Composable
private fun DurationWheelPreview() {
    SleepRelaxTheme {
        DurationWheel(
            state = rememberLazyListState(initialFirstVisibleItemIndex = 1),
            values = PREVIEW_MINUTE_VALUES,
            label = "Minutes",
            valueLabel = { "$it min" },
        )
    }
}
