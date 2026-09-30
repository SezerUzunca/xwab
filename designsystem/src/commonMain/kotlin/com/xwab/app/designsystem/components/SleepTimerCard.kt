package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xwab.app.designsystem.theme.SleepRelaxTheme

/**
 * The session's sleep timer in a card, on a content screen.
 *
 * It acts on the session, so on whatever is playing, not only on the item this card sits under, and
 * it can be set before pressing play. Stateless: the caller observes the timer and owns every
 * command.
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
