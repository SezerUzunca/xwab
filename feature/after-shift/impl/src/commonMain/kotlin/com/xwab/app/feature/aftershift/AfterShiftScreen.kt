package com.xwab.app.feature.aftershift

import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.session.port.PlaybackFailure
import com.xwab.app.designsystem.components.LoadingContent
import com.xwab.app.designsystem.components.PlayPauseButton
import com.xwab.app.designsystem.components.ScreenContainer
import com.xwab.app.designsystem.components.SleepTimerCard
import com.xwab.app.designsystem.components.glassCard
import com.xwab.app.designsystem.components.screenContentPadding
import com.xwab.app.designsystem.theme.SleepRelaxTheme
import com.xwab.app.feature.aftershift.navigation.AfterShiftRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import xwab.feature.after_shift.generated.resources.Res
import xwab.feature.after_shift.generated.resources.after_shift_guide_could_not_play
import xwab.feature.after_shift.generated.resources.after_shift_guide_minutes
import xwab.feature.after_shift.generated.resources.after_shift_guide_not_found
import xwab.feature.after_shift.generated.resources.after_shift_guide_preparing
import xwab.feature.after_shift.generated.resources.after_shift_guide_unavailable
import xwab.feature.after_shift.generated.resources.after_shift_guides_empty
import xwab.feature.after_shift.generated.resources.after_shift_guides_title
import xwab.feature.after_shift.generated.resources.after_shift_research_note
import xwab.feature.after_shift.generated.resources.after_shift_safety
import xwab.feature.after_shift.generated.resources.after_shift_subtitle
import xwab.feature.after_shift.generated.resources.after_shift_tip_cool
import xwab.feature.after_shift.generated.resources.after_shift_tip_dark
import xwab.feature.after_shift.generated.resources.after_shift_tip_quiet
import xwab.feature.after_shift.generated.resources.after_shift_tips_title
import xwab.feature.after_shift.generated.resources.after_shift_title

@Composable
internal fun AfterShiftScreenRoute(viewModel: AfterShiftViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Another tap on the tab at its root asks for the start, as every tab's root does.
    ResultEffect<AfterShiftRoute> { scope.launch { scrollState.animateScrollTo(0) } }

    when (val content = state) {
        AfterShiftUiState.Loading -> LoadingContent()
        is AfterShiftUiState.Ready -> AfterShiftScreen(
            state = content.value,
            note = note,
            actions = AfterShiftActions(
                onNoteChange = viewModel::onNoteChange,
                onNoteSave = viewModel::saveNote,
                onNoteClear = viewModel::clearNote,
                onPlaybackClick = viewModel::togglePlayback,
                onTimerStart = viewModel::startSleepTimer,
                onTimerCancel = viewModel::cancelSleepTimer,
            ),
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            scrollState = scrollState,
        )
    }
}

/** What the screen can ask for, gathered so the screen takes one parameter for all of it. */
internal class AfterShiftActions(
    val onNoteChange: (String) -> Unit,
    val onNoteSave: () -> Unit,
    val onNoteClear: () -> Unit,
    val onPlaybackClick: (GuideId) -> Unit,
    val onTimerStart: (Long) -> Unit,
    val onTimerCancel: () -> Unit,
)

/**
 * A tab's root, read top to bottom in the order the time after a night shift runs: get home, put
 * tomorrow's errands down somewhere, then lie down with a guide and the timer. Below them, a few
 * plain points about the room, and where the guides come from.
 */
@Composable
internal fun AfterShiftScreen(
    state: AfterShiftState,
    note: ParkedNote,
    actions: AfterShiftActions,
    sleepTimerRemainingMs: Long? = null,
    scrollState: ScrollState = rememberScrollState(),
) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(screenContentPadding()),
        ) {
            Header()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            ParkedNoteCard(note, actions)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Guides(state, actions.onPlaybackClick)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            SleepTimerCard(
                remainingMs = sleepTimerRemainingMs,
                onTimerStart = actions.onTimerStart,
                onTimerCancel = actions.onTimerCancel,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Tips()
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = stringResource(Res.string.after_shift_title),
        modifier = Modifier.semantics { heading() },
        // The same size as the other tab titles; a tab root is not a sub-page.
        style = SleepRelaxTheme.typography.headlineLarge,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
    Text(
        text = stringResource(Res.string.after_shift_subtitle),
        style = SleepRelaxTheme.typography.bodyLarge,
        color = SleepRelaxTheme.colors.textSecondary,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    Text(
        text = stringResource(Res.string.after_shift_safety),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.accent,
    )
}

@Composable
private fun Guides(state: AfterShiftState, onPlaybackClick: (GuideId) -> Unit) {
    SectionTitle(Res.string.after_shift_guides_title)
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    if (state.guides.isEmpty()) {
        Text(
            stringResource(Res.string.after_shift_guides_empty),
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    state.guides.forEach { guide ->
        GuideCard(guide, state, onPlaybackClick = { onPlaybackClick(guide.id) })
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    }
}

@Composable
private fun GuideCard(guide: Guide, state: AfterShiftState, onPlaybackClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    guide.title,
                    style = SleepRelaxTheme.typography.titleMedium,
                    color = SleepRelaxTheme.colors.textPrimary,
                )
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                Text(
                    guide.description,
                    style = SleepRelaxTheme.typography.bodyMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
                Text(
                    stringResource(Res.string.after_shift_guide_minutes, guide.minutes),
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.width(SleepRelaxTheme.dimens.spacingMedium))
            PlayPauseButton(
                playRequested = state.isGuidePlaying(guide.id),
                onClick = onPlaybackClick,
                contentTitle = guide.title,
            )
        }
        GuideStatus(guide.id, state)
    }
}

@Composable
private fun GuideStatus(guideId: GuideId, state: AfterShiftState) {
    if (state.isGuidePreparing(guideId)) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(Res.string.after_shift_guide_preparing),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    state.guideFailure(guideId)?.let { failure ->
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
private fun Tips() {
    SectionTitle(Res.string.after_shift_tips_title)
    listOf(
        Res.string.after_shift_tip_dark,
        Res.string.after_shift_tip_quiet,
        Res.string.after_shift_tip_cool,
    ).forEach { tip ->
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            "• ${stringResource(tip)}",
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textPrimary,
        )
    }
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
    Text(
        stringResource(Res.string.after_shift_research_note),
        style = SleepRelaxTheme.typography.labelMedium,
        color = SleepRelaxTheme.colors.textSecondary,
    )
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
 * A guide the catalog dropped, one that could not be spoken and one the player could not open call
 * for different advice: only the second is worth checking the device's voice for.
 */
internal fun PlaybackFailure.messageResource(): StringResource = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.after_shift_guide_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.after_shift_guide_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.after_shift_guide_could_not_play
}


@Preview
@Composable
private fun AfterShiftScreenPreview() {
    SleepRelaxTheme {
        AfterShiftScreen(
            state = AfterShiftState(
                guides = listOf(
                    Guide(
                        id = GuideId("muscle-relaxation"),
                        title = "Muscle Relaxation",
                        description = "Tense and release each part of your body in turn. A good place to start.",
                        minutes = 8,
                        languageTag = "en-US",
                    ),
                ),
            ),
            note = ParkedNote(draft = "Call the bank"),
            actions = AfterShiftActions({}, {}, {}, {}, {}, {}),
        )
    }
}
