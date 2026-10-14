package com.xwab.app.feature.cantsleep

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
import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuideUse
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
import xwab.feature.cant_sleep.generated.resources.Res
import xwab.feature.cant_sleep.generated.resources.cant_sleep_brightness
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guide_could_not_play
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guide_minutes
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guide_not_found
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guide_preparing
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guide_unavailable
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guides_empty
import xwab.feature.cant_sleep.generated.resources.cant_sleep_guides_title
import xwab.feature.cant_sleep.generated.resources.cant_sleep_research_note
import xwab.feature.cant_sleep.generated.resources.cant_sleep_step_bed
import xwab.feature.cant_sleep.generated.resources.cant_sleep_step_elsewhere
import xwab.feature.cant_sleep.generated.resources.cant_sleep_step_get_up
import xwab.feature.cant_sleep.generated.resources.cant_sleep_step_quiet
import xwab.feature.cant_sleep.generated.resources.cant_sleep_step_return
import xwab.feature.cant_sleep.generated.resources.cant_sleep_steps_title
import xwab.feature.cant_sleep.generated.resources.cant_sleep_subtitle
import xwab.feature.cant_sleep.generated.resources.cant_sleep_title

@Composable
internal fun CantSleepScreenRoute(viewModel: CantSleepViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val content = state) {
        CantSleepUiState.Loading -> LoadingContent()
        is CantSleepUiState.Ready -> CantSleepScreen(
            state = content.value,
            onBack = onBack,
            onPlaybackClick = viewModel::togglePlayback,
        )
    }
}

/**
 * Read in the order the listener needs it, sitting up in a dim room: what to do, then the guides to
 * hear while they wait for sleepiness. There is no sleep timer: a guide here plays once, and the
 * listener goes back to bed when they feel sleepy, not when a clock says so.
 */
@Composable
internal fun CantSleepScreen(
    state: CantSleepState,
    onBack: () -> Unit,
    onPlaybackClick: (GuideId) -> Unit,
) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding()),
        ) {
            Row(Modifier.fillMaxWidth()) { BackButton(onClick = dropUnlessResumed(block = onBack)) }
            Header()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            Steps()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Guides(state, onPlaybackClick)
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingMedium))
            Text(
                stringResource(Res.string.cant_sleep_brightness),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.accent,
            )
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Text(
                stringResource(Res.string.cant_sleep_research_note),
                style = SleepRelaxTheme.typography.labelMedium,
                color = SleepRelaxTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = stringResource(Res.string.cant_sleep_title),
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.headlineLarge,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
    Text(
        text = stringResource(Res.string.cant_sleep_subtitle),
        style = SleepRelaxTheme.typography.bodyLarge,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}

@Composable
private fun Steps() {
    SectionTitle(Res.string.cant_sleep_steps_title)
    listOf(
        Res.string.cant_sleep_step_get_up,
        Res.string.cant_sleep_step_elsewhere,
        Res.string.cant_sleep_step_quiet,
        Res.string.cant_sleep_step_return,
        Res.string.cant_sleep_step_bed,
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
private fun Guides(state: CantSleepState, onPlaybackClick: (GuideId) -> Unit) {
    SectionTitle(Res.string.cant_sleep_guides_title)
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
    if (state.guides.isEmpty()) {
        Text(
            stringResource(Res.string.cant_sleep_guides_empty),
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
private fun GuideCard(guide: Guide, state: CantSleepState, onPlaybackClick: () -> Unit) {
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
                    stringResource(Res.string.cant_sleep_guide_minutes, guide.minutes),
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
private fun GuideStatus(guideId: GuideId, state: CantSleepState) {
    if (state.isGuidePreparing(guideId)) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(Res.string.cant_sleep_guide_preparing),
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
    is PlaybackFailure.ItemNotFound -> Res.string.cant_sleep_guide_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.cant_sleep_guide_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.cant_sleep_guide_could_not_play
}

@Preview
@Composable
private fun CantSleepScreenPreview() {
    SleepRelaxTheme {
        CantSleepScreen(
            state = CantSleepState(
                guides = listOf(
                    Guide(
                        id = GuideId("out-of-bed"),
                        title = "Out of Bed",
                        description = "A calm, unhurried voice to keep you company until you feel sleepy again.",
                        minutes = 8,
                        languageTag = "en-US",
                        use = GuideUse.OutOfBed,
                    ),
                ),
            ),
            onBack = {},
            onPlaybackClick = {},
        )
    }
}
