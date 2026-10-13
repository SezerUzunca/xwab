package com.xwab.app.feature.rehearsal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.designsystem.components.BackButton
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.feature.rehearsal.generated.resources.Res
import xwab.feature.rehearsal.generated.resources.rehearsal_could_not_play
import xwab.feature.rehearsal.generated.resources.rehearsal_listen_body
import xwab.feature.rehearsal.generated.resources.rehearsal_listen_empty
import xwab.feature.rehearsal.generated.resources.rehearsal_listen_title
import xwab.feature.rehearsal.generated.resources.rehearsal_not_found
import xwab.feature.rehearsal.generated.resources.rehearsal_preparing
import xwab.feature.rehearsal.generated.resources.rehearsal_research_note
import xwab.feature.rehearsal.generated.resources.rehearsal_safety
import xwab.feature.rehearsal.generated.resources.rehearsal_step_change
import xwab.feature.rehearsal.generated.resources.rehearsal_step_choose
import xwab.feature.rehearsal.generated.resources.rehearsal_step_daily
import xwab.feature.rehearsal.generated.resources.rehearsal_step_short
import xwab.feature.rehearsal.generated.resources.rehearsal_steps_title
import xwab.feature.rehearsal.generated.resources.rehearsal_subtitle
import xwab.feature.rehearsal.generated.resources.rehearsal_title
import xwab.feature.rehearsal.generated.resources.rehearsal_unavailable

@Composable
internal fun RehearsalScreenRoute(viewModel: RehearsalViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The field reads the keystroke's own value, not the one that comes back through the state.
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    when (val content = state) {
        RehearsalUiState.Loading -> LoadingContent()
        is RehearsalUiState.Ready -> RehearsalScreen(
            state = content.value.copy(draft = draft),
            actions = RehearsalActions(
                onBack = onBack,
                onDraftChange = viewModel::onDraftChange,
                onSave = viewModel::save,
                onClear = viewModel::clear,
                onPlaybackClick = viewModel::togglePlayback,
            ),
        )
    }
}

/** What the screen can ask for, gathered so the screen takes one parameter for all of it. */
internal class RehearsalActions(
    val onBack: () -> Unit,
    val onDraftChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onClear: () -> Unit,
    val onPlaybackClick: () -> Unit,
)

/**
 * Read top to bottom in the order the exercise runs: what to do, the new dream written down, then
 * listening to it. The safety note sits right under the steps, before anything is written, and the
 * source of the technique at the end. There is no sleep timer: rehearsal is practised awake.
 */
@Composable
internal fun RehearsalScreen(state: RehearsalState, actions: RehearsalActions) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding()),
        ) {
            Row(Modifier.fillMaxWidth()) { BackButton(onClick = dropUnlessResumed(block = actions.onBack)) }
            Header()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            Steps()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingMedium))
            Text(
                stringResource(Res.string.rehearsal_safety),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.accent,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            ScriptCard(state, actions)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            ListenCard(state, actions.onPlaybackClick)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Text(
                stringResource(Res.string.rehearsal_research_note),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = stringResource(Res.string.rehearsal_title),
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.headlineLarge,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
    Text(
        text = stringResource(Res.string.rehearsal_subtitle),
        style = SleepRelaxTheme.typography.bodyLarge,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}

@Composable
private fun Steps() {
    SectionTitle(Res.string.rehearsal_steps_title)
    listOf(
        Res.string.rehearsal_step_choose,
        Res.string.rehearsal_step_change,
        Res.string.rehearsal_step_short,
        Res.string.rehearsal_step_daily,
    ).forEachIndexed { index, step ->
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            "${index + 1}. ${stringResource(step)}",
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun ListenCard(state: RehearsalState, onPlaybackClick: () -> Unit) {
    val title = stringResource(Res.string.rehearsal_listen_title)
    Column(Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = SleepRelaxTheme.typography.titleMedium, color = SleepRelaxTheme.colors.textPrimary)
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                Text(
                    stringResource(
                        if (state.canPlay) Res.string.rehearsal_listen_body else Res.string.rehearsal_listen_empty,
                    ),
                    style = SleepRelaxTheme.typography.bodyMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.width(SleepRelaxTheme.dimens.spacingMedium))
            PlayPauseButton(
                playRequested = state.isPlaying,
                onClick = onPlaybackClick,
                enabled = state.canPlay,
                contentTitle = title,
            )
        }
        ListenStatus(state)
    }
}

@Composable
private fun ListenStatus(state: RehearsalState) {
    if (state.isPreparing) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(Res.string.rehearsal_preparing),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    state.playbackFailure?.let { failure ->
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(failure.messageResource()),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.error,
        )
    }
}

@Composable
private fun SectionTitle(title: StringResource) {
    Text(
        stringResource(title),
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.titleMedium,
        color = SleepRelaxTheme.colors.textPrimary,
    )
}

/**
 * A wording that is gone, one that could not be spoken and one the player could not open call for
 * different advice: only the second is worth checking the device's voice for.
 */
internal fun PlaybackFailure.messageResource(): StringResource = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.rehearsal_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.rehearsal_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.rehearsal_could_not_play
}

@Preview
@Composable
private fun RehearsalScreenPreview() {
    SleepRelaxTheme {
        RehearsalScreen(
            state = RehearsalState(draft = "I am back on the beach, but this time the wave sets me down gently."),
            actions = RehearsalActions({}, {}, {}, {}, {}),
        )
    }
}
