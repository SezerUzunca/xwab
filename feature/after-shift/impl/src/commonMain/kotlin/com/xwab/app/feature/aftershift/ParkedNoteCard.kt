package com.xwab.app.feature.aftershift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import com.xwab.app.designsystem.components.SleepRelaxTextButton
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.stringResource
import xwab.feature.after_shift.generated.resources.Res
import xwab.feature.after_shift.generated.resources.after_shift_note_body
import xwab.feature.after_shift.generated.resources.after_shift_note_clear
import xwab.feature.after_shift.generated.resources.after_shift_note_failed
import xwab.feature.after_shift.generated.resources.after_shift_note_hint
import xwab.feature.after_shift.generated.resources.after_shift_note_label
import xwab.feature.after_shift.generated.resources.after_shift_note_save
import xwab.feature.after_shift.generated.resources.after_shift_note_saved
import xwab.feature.after_shift.generated.resources.after_shift_note_title

/**
 * Where the listener puts down what they would otherwise lie awake rehearsing: a field, and the
 * two things to do with it. The text is kept until it is saved or cleared, even when a save fails.
 */
@Composable
internal fun ParkedNoteCard(note: ParkedNote, actions: AfterShiftActions) {
    Column(Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge)) {
        Text(
            stringResource(Res.string.after_shift_note_title),
            modifier = Modifier.semantics { heading() },
            style = SleepRelaxTheme.typography.titleMedium,
            color = SleepRelaxTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
        Text(
            stringResource(Res.string.after_shift_note_body),
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingMedium))
        NoteField(note.draft, actions.onNoteChange)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteStatusLine(note.status, Modifier.weight(1f))
            SleepRelaxTextButton(onClick = actions.onNoteClear, enabled = note.canClear) {
                Text(stringResource(Res.string.after_shift_note_clear))
            }
            Spacer(Modifier.width(SleepRelaxTheme.dimens.spacingSmall))
            SleepRelaxTextButton(onClick = actions.onNoteSave, enabled = note.canSave) {
                Text(stringResource(Res.string.after_shift_note_save))
            }
        }
    }
}

/**
 * The note's field, drawn here rather than taken from Material: `OutlinedTextField` throws while
 * measuring under this project's Material 3 alpha. Its label sits above it, so it stays readable
 * whatever is typed.
 */
@Composable
private fun NoteField(text: String, onChange: (String) -> Unit) {
    val label = stringResource(Res.string.after_shift_note_label)
    Text(label, style = SleepRelaxTheme.typography.labelMedium, color = SleepRelaxTheme.colors.textSecondary)
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
    BasicTextField(
        value = text,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NOTE_MIN_HEIGHT)
            .border(NOTE_BORDER, SleepRelaxTheme.colors.textSecondary, SleepRelaxTheme.shapes.small)
            .padding(SleepRelaxTheme.dimens.spacingMedium)
            .semantics { contentDescription = label },
        textStyle = SleepRelaxTheme.typography.bodyLarge.copy(color = SleepRelaxTheme.colors.textPrimary),
        cursorBrush = SolidColor(SleepRelaxTheme.colors.accent),
        decorationBox = { field ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        stringResource(Res.string.after_shift_note_hint),
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
private fun NoteStatusLine(status: NoteStatus, modifier: Modifier) {
    val (message, color) = when (status) {
        NoteStatus.Idle -> null to SleepRelaxTheme.colors.textSecondary
        NoteStatus.Saved -> Res.string.after_shift_note_saved to SleepRelaxTheme.colors.textSecondary
        NoteStatus.Failed -> Res.string.after_shift_note_failed to SleepRelaxTheme.colors.error
    }
    Text(
        text = message?.let { stringResource(it) }.orEmpty(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = SleepRelaxTheme.typography.labelMedium,
        color = color,
    )
}

/** Room for a few lines before the field starts to scroll. */
private val NOTE_MIN_HEIGHT = 112.dp
private val NOTE_BORDER = 1.dp
