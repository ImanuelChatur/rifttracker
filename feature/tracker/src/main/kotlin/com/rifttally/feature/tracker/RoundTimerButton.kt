package com.rifttally.feature.tracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftFloatingToolbar
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * The round timer on the control bar: "mm:ss" left (tabular digits), amber in the last 5 minutes, then
 * "+mm:ss" overtime in the error color. Tap to start or pause; a play/pause glyph shows which a tap does.
 *
 * Crossing 0 while on screen gives one strong haptic (if [hapticsEnabled]); reaching 5 minutes and 0 are
 * announced politely to screen readers, nothing else (not every second).
 */
@Composable
internal fun RoundTimerButton(
    timer: RoundTimerUiState,
    hapticsEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TimeUpHaptic(timer = timer, enabled = hapticsEnabled)
    val color = when (timer.phase) {
        RoundTimerPhase.Normal -> Color.Unspecified
        RoundTimerPhase.Warning -> RiftTheme.tableColors.timerWarning
        RoundTimerPhase.Overtime -> RiftTheme.colorScheme.error
    }
    val time = if (timer.phase == RoundTimerPhase.Overtime) {
        stringResource(R.string.feature_tracker_timer_overtime, timer.minutesPart, timer.secondsPart)
    } else {
        stringResource(R.string.feature_tracker_timer_time, timer.minutesPart, timer.secondsPart)
    }
    val description = timerDescription(timer)
    val state = stringResource(
        if (timer.isRunning) R.string.feature_tracker_timer_running else R.string.feature_tracker_timer_paused,
    )
    val actionLabel = stringResource(
        if (timer.isRunning) R.string.feature_tracker_timer_pause else R.string.feature_tracker_timer_start,
    )
    val announcement = when (timer.phase) {
        RoundTimerPhase.Normal -> null
        RoundTimerPhase.Warning -> stringResource(R.string.feature_tracker_timer_five_minutes_left)
        RoundTimerPhase.Overtime -> stringResource(R.string.feature_tracker_timer_up)
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Polite live region whose text changes only at 5:00 and at 0, so those two moments are announced
        // and the per-second ticks are not (the button's own description has no live region).
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    if (announcement != null) contentDescription = announcement
                },
        )
        Row(
            modifier = Modifier
                .sizeIn(minWidth = RiftTheme.sizes.minTouchTarget, minHeight = RiftTheme.sizes.minTouchTarget)
                .clip(RiftTheme.shapes.full)
                .testTag(ROUND_TIMER_TEST_TAG)
                // Replaces the clickable's (and the children's) semantics: one labeled button.
                .clearAndSetSemantics {
                    contentDescription = description
                    stateDescription = state
                    role = Role.Button
                    onClick(label = actionLabel) {
                        onToggle()
                        true
                    }
                }
                .clickable(onClick = onToggle)
                .padding(horizontal = RiftTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
        ) {
            Icon(
                imageVector = if (timer.isRunning) RiftIcons.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (color == Color.Unspecified) RiftTheme.colorScheme.onSurfaceVariant else color,
                modifier = Modifier.size(RiftTheme.sizes.iconSmall),
            )
            Text(text = time, style = RiftTheme.scoreTypography.counter, color = color)
        }
    }
}

/** "12 minutes 30 seconds left in round", or "2 minutes 13 seconds over time". */
@Composable
private fun timerDescription(timer: RoundTimerUiState): String {
    val minutes = timer.minutesPart.toInt()
    val seconds = timer.secondsPart.toInt()
    val minutesText = pluralStringResource(R.plurals.feature_tracker_timer_minutes, minutes, minutes)
    val secondsText = pluralStringResource(R.plurals.feature_tracker_timer_seconds, seconds, seconds)
    val duration = when {
        minutes == 0 -> secondsText
        seconds == 0 -> minutesText
        else -> stringResource(R.string.feature_tracker_timer_minutes_seconds, minutesText, secondsText)
    }
    return if (timer.phase == RoundTimerPhase.Overtime) {
        stringResource(R.string.feature_tracker_timer_over_description, duration)
    } else {
        stringResource(R.string.feature_tracker_timer_left_description, duration)
    }
}

/**
 * One strong haptic when a running timer crosses 0 while on screen. Remembered per composition, so
 * opening a game that is already in overtime (or rotating) doesn't buzz again.
 */
@Composable
private fun TimeUpHaptic(timer: RoundTimerUiState, enabled: Boolean) {
    val haptics = LocalHapticFeedback.current
    var lastPhase by remember { mutableStateOf<RoundTimerPhase?>(null) }
    LaunchedEffect(timer.phase) {
        val previous = lastPhase
        lastPhase = timer.phase
        // Only a crossing seen live: coming back to the app minutes after 0 doesn't buzz late.
        val crossedZero = previous != null && previous != RoundTimerPhase.Overtime &&
            timer.phase == RoundTimerPhase.Overtime && timer.seconds <= TIME_UP_HAPTIC_WINDOW_SECONDS
        if (crossedZero && timer.isRunning && enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

internal const val ROUND_TIMER_TEST_TAG = "round_timer"

/** The time-up haptic fires only within this many seconds after 0. */
private const val TIME_UP_HAPTIC_WINDOW_SECONDS = 2

@PreviewRiftThemes
@Composable
private fun RoundTimerButtonPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            listOf(
                RoundTimerUiState(isRunning = false, seconds = 3_000, phase = RoundTimerPhase.Normal),
                RoundTimerUiState(isRunning = true, seconds = 272, phase = RoundTimerPhase.Warning),
                RoundTimerUiState(isRunning = true, seconds = 133, phase = RoundTimerPhase.Overtime),
            ).forEach { timer ->
                RiftFloatingToolbar {
                    RoundTimerButton(timer = timer, hapticsEnabled = false, onToggle = {})
                }
            }
        }
    }
}
