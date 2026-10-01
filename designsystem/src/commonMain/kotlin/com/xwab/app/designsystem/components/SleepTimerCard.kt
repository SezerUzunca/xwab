package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/**
 * The session's sleep timer in a card, on a content screen.
 *
 * It acts on the session, so on whatever is playing, not only on the item this card sits under, and
 * it can be set before pressing play. The caller observes the timer and owns every command;
 * the control keeps only which preset was tapped.
 */
@Composable
fun SleepTimerCard(
    remainingMs: Long?,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SleepTimerControl(
        remainingMs = remainingMs,
        onTimerStart = onTimerStart,
        onTimerCancel = onTimerCancel,
        modifier = modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge),
    )
}

@Preview(name = "Timer running", widthDp = 360)
@Composable
private fun SleepTimerCardRunningPreview() {
    SleepRelaxTheme {
        SleepTimerCard(remainingMs = 1_740_000L, onTimerStart = {}, onTimerCancel = {})
    }
}

@Preview(name = "Timer off", widthDp = 360)
@Composable
private fun SleepTimerCardOffPreview() {
    SleepRelaxTheme {
        SleepTimerCard(remainingMs = null, onTimerStart = {}, onTimerCancel = {})
    }
}
