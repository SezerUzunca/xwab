package com.xwab.app.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.designsystem.generated.resources.Res
import xwab.designsystem.generated.resources.repeat_playback

/**
 * The session's settings on a content screen: the sleep timer, then repeat.
 *
 * Both act on the session, so on whatever is playing, not only on the item this card sits under —
 * the timer stops whatever plays, and repeat is carried to the next item played. Stateless: the
 * caller observes both values and owns every command.
 */
@Composable
fun PlaybackSettingsCard(
    sleepTimerRemainingMs: Long?,
    isLooping: Boolean,
    onTimerStart: (Long) -> Unit,
    onTimerCancel: () -> Unit,
    onLoopingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(SleepRelaxTheme.dimens.spacingMedium),
    ) {
        SleepTimerControl(
            remainingMs = sleepTimerRemainingMs,
            onTimerStart = onTimerStart,
            onTimerCancel = onTimerCancel,
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .toggleable(value = isLooping, role = Role.Switch, onValueChange = onLoopingChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.repeat_playback),
                modifier = Modifier.weight(1f),
                style = SleepRelaxTheme.typography.titleMedium,
                color = SleepRelaxTheme.colors.textPrimary,
            )
            SleepRelaxSwitch(checked = isLooping, onCheckedChange = null)
        }
    }
}
