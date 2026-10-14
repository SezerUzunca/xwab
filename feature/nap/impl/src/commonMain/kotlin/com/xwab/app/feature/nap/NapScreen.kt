package com.xwab.app.feature.nap

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
import com.xwab.app.core.nap.port.Nap
import com.xwab.app.core.nap.port.NapId
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
import xwab.feature.nap.generated.resources.Res
import xwab.feature.nap.generated.resources.nap_choose_title
import xwab.feature.nap.generated.resources.nap_could_not_play
import xwab.feature.nap.generated.resources.nap_not_alarm_body
import xwab.feature.nap.generated.resources.nap_not_alarm_title
import xwab.feature.nap.generated.resources.nap_not_found
import xwab.feature.nap.generated.resources.nap_preparing
import xwab.feature.nap.generated.resources.nap_research_note
import xwab.feature.nap.generated.resources.nap_spoken
import xwab.feature.nap.generated.resources.nap_subtitle
import xwab.feature.nap.generated.resources.nap_tip_shift
import xwab.feature.nap.generated.resources.nap_tip_short
import xwab.feature.nap.generated.resources.nap_tip_start
import xwab.feature.nap.generated.resources.nap_tip_volume
import xwab.feature.nap.generated.resources.nap_tips_title
import xwab.feature.nap.generated.resources.nap_title
import xwab.feature.nap.generated.resources.nap_unavailable

@Composable
internal fun NapScreenRoute(viewModel: NapViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val content = state) {
        NapUiState.Loading -> LoadingContent()
        is NapUiState.Ready -> NapScreen(
            state = content.value,
            onBack = onBack,
            onPlaybackClick = viewModel::togglePlayback,
        )
    }
}

/**
 * The warning first, before any nap can be started: the wake-up is the end of a track, not an
 * alarm. Then the lengths, a few plain points, and where the advice comes from. There is no sleep
 * timer card: a nap ends with its own wake-up, and starting one cancels the session's timer.
 */
@Composable
internal fun NapScreen(state: NapState, onBack: () -> Unit, onPlaybackClick: (NapId) -> Unit) {
    ScreenContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding()),
        ) {
            Row(Modifier.fillMaxWidth()) { BackButton(onClick = dropUnlessResumed(block = onBack)) }
            Header()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingLarge))
            NotAnAlarm()
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            SectionTitle(Res.string.nap_choose_title)
            state.naps.forEach { nap ->
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
                NapCard(nap, state, onPlaybackClick = { onPlaybackClick(nap.id) })
            }
            Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraLarge))
            Tips()
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = stringResource(Res.string.nap_title),
        modifier = Modifier.semantics { heading() },
        style = SleepRelaxTheme.typography.headlineLarge,
        color = SleepRelaxTheme.colors.textPrimary,
    )
    Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
    Text(
        text = stringResource(Res.string.nap_subtitle),
        style = SleepRelaxTheme.typography.bodyLarge,
        color = SleepRelaxTheme.colors.textSecondary,
    )
}

@Composable
private fun NotAnAlarm() {
    Column(Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge)) {
        Text(
            stringResource(Res.string.nap_not_alarm_title),
            modifier = Modifier.semantics { heading() },
            style = SleepRelaxTheme.typography.titleMedium,
            color = SleepRelaxTheme.colors.accent,
        )
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
        Text(
            stringResource(Res.string.nap_not_alarm_body),
            style = SleepRelaxTheme.typography.bodyMedium,
            color = SleepRelaxTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun NapCard(nap: Nap, state: NapState, onPlaybackClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().glassCard().padding(SleepRelaxTheme.dimens.spacingLarge)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    nap.title,
                    style = SleepRelaxTheme.typography.titleMedium,
                    color = SleepRelaxTheme.colors.textPrimary,
                )
                Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingExtraSmall))
                Text(
                    nap.description,
                    style = SleepRelaxTheme.typography.bodyMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
                Text(
                    stringResource(Res.string.nap_spoken),
                    style = SleepRelaxTheme.typography.labelMedium,
                    color = SleepRelaxTheme.colors.textSecondary,
                )
            }
            Spacer(Modifier.width(SleepRelaxTheme.dimens.spacingMedium))
            PlayPauseButton(
                playRequested = state.isNapPlaying(nap.id),
                onClick = onPlaybackClick,
                contentTitle = nap.title,
            )
        }
        NapStatus(nap.id, state)
    }
}

@Composable
private fun NapStatus(napId: NapId, state: NapState) {
    if (state.isNapPreparing(napId)) {
        Spacer(Modifier.height(SleepRelaxTheme.dimens.spacingSmall))
        Text(
            stringResource(Res.string.nap_preparing),
            style = SleepRelaxTheme.typography.labelMedium,
            color = SleepRelaxTheme.colors.textSecondary,
        )
    }
    state.napFailure(napId)?.let { failure ->
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
    SectionTitle(Res.string.nap_tips_title)
    listOf(
        Res.string.nap_tip_start,
        Res.string.nap_tip_volume,
        Res.string.nap_tip_shift,
        Res.string.nap_tip_short,
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
        stringResource(Res.string.nap_research_note),
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
 * A nap the catalog dropped, one that could not be spoken and one the player could not open call
 * for different advice: only the second is worth checking the device's voice for.
 */
internal fun PlaybackFailure.messageResource(): StringResource = when (this) {
    is PlaybackFailure.ItemNotFound -> Res.string.nap_not_found
    is PlaybackFailure.SourceUnavailable -> Res.string.nap_unavailable
    is PlaybackFailure.EngineFailed -> Res.string.nap_could_not_play
}

@Preview
@Composable
private fun NapScreenPreview() {
    SleepRelaxTheme {
        NapScreen(
            state = NapState(
                naps = listOf(
                    previewNap(minutes = 10, description = "The shortest: a quick lift."),
                    previewNap(minutes = 20, description = "The longest short nap."),
                ),
            ),
            onBack = {},
            onPlaybackClick = {},
        )
    }
}

private fun previewNap(minutes: Int, description: String) =
    Nap(NapId("nap-$minutes"), "$minutes-Minute Nap", description, minutes, "en-US")
