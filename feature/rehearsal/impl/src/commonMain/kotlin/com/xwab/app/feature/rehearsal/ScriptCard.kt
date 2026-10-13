package com.xwab.app.feature.rehearsal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xwab.app.core.rehearsal.port.REHEARSAL_SCRIPT_MAX_LENGTH
import com.xwab.app.designsystem.components.SleepRelaxTextButton
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.feature.rehearsal.generated.resources.Res
import xwab.feature.rehearsal.generated.resources.rehearsal_clear
import xwab.feature.rehearsal.generated.resources.rehearsal_field_count
import xwab.feature.rehearsal.generated.resources.rehearsal_field_hint
import xwab.feature.rehearsal.generated.resources.rehearsal_field_label
import xwab.feature.rehearsal.generated.resources.rehearsal_save
import xwab.feature.rehearsal.generated.resources.rehearsal_save_failed
import xwab.feature.rehearsal.generated.resources.rehearsal_saved

/** The new dream, and the two things to do with it. The text is kept until saved or cleared, even when a save fails. */
@Composable
internal fun ScriptCard(state: RehearsalState, actions: RehearsalActions) {
    Column(Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge)) {
        ScriptField(state.draft, actions.onDraftChange)
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
        Text(
            stringResource(Res.string.rehearsal_field_count, state.draft.length, REHEARSAL_SCRIPT_MAX_LENGTH),
            modifier = Modifier.align(Alignment.End),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaveStatusLine(state.saveStatus, Modifier.weight(1f))
            SleepRelaxTextButton(onClick = actions.onClear, enabled = state.canClear) {
                Text(stringResource(Res.string.rehearsal_clear))
            }
            Spacer(Modifier.width(SleepRelaxTheme.dimens.spacingSmall))
            SleepRelaxTextButton(onClick = actions.onSave, enabled = state.canSave) {
                Text(stringResource(Res.string.rehearsal_save))
            }
        }
    }
}

/**
 * Drawn here rather than taken from Material: `OutlinedTextField` throws while measuring under this
 * project's Material 3 alpha. Its label sits above it, so it stays readable whatever is typed.
 */
@Composable
private fun ScriptField(text: String, onChange: (String) -> Unit) {
    val label = stringResource(Res.string.rehearsal_field_label)
    Text(
        label,
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.titleMedium,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    BasicTextField(
        value = text,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FIELD_MIN_HEIGHT)
            .border(FIELD_BORDER, SleepRelaxTheme.colors.textSecondary, SleepRelaxTheme.shapes.small)
            .padding(SleepRelaxTheme.dimens.spacingMedium)
            .semantics { contentDescription = label },
        textStyle = SleepRelaxTheme.typography.bodyLarge.copy(color = SleepRelaxTheme.colors.textPrimary),
        cursorBrush = SolidColor(SleepRelaxTheme.colors.accent),
        decorationBox = { field ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        stringResource(Res.string.rehearsal_field_hint),
                        style = SleepRelaxTheme.typography.bodyLarge,
                        color = SleepRelaxTheme.colors.textSecondary,
                    )
                }
                field()
            }
        },
    )
}

@Composable
private fun SaveStatusLine(status: SaveStatus, modifier: Modifier) {
    val (message, color) = when (status) {
        SaveStatus.Idle -> null to SleepRelaxTheme.colors.textSecondary
        SaveStatus.Saved -> Res.string.rehearsal_saved to SleepRelaxTheme.colors.textSecondary
        SaveStatus.Failed -> Res.string.rehearsal_save_failed to SleepRelaxTheme.colors.error
    }
    Text(
        text = message?.let { stringResource(it) }.orEmpty(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = SleepRelaxTheme.typography.labelMedium,
        color = color,
    )
}

/** Room for a short paragraph before the field starts to scroll. */
private val FIELD_MIN_HEIGHT = 160.dp
private val FIELD_BORDER = 1.dp
