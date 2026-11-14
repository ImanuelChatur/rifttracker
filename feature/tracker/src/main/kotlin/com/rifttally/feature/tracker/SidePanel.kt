package com.rifttally.feature.tracker

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import com.rifttally.core.ui.CounterRow
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.PlayerNameChip
import com.rifttally.core.ui.RepeatingClickBox
import kotlinx.collections.immutable.persistentListOf

/**
 * One side's panel: the Legend portrait with the score on it, the name, and two large tap zones
 * (left half −, right half +, each with a visible icon). Holding a zone repeats. The caller turns the
 * panel toward its seat (see [TrackerTableLayout]).
 */
@Composable
internal fun SidePanel(
    panel: SidePanelUiState,
    hapticsEnabled: Boolean,
    onPointsChange: (delta: Int) -> Unit,
    onCounterChange: (PlayerId, CounterId, delta: Int) -> Unit,
    onCounterRemove: (PlayerId, CounterId) -> Unit,
    onAddCounterClick: (PlayerId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstPlayer = panel.players.firstOrNull()
    val playerColor = firstPlayer?.colorId?.let(PlayerColors::fromId) ?: PlayerColors.Grey
    val haptics = LocalHapticFeedback.current
    val change: (Int) -> Unit = { delta ->
        if (hapticsEnabled &&
            (delta > 0 || panel.points > 0)
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        onPointsChange(delta)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RiftTheme.spacing.xl),
        color = RiftTheme.colorScheme.surfaceContainer,
        border = if (panel.isAtTarget) {
            BorderStroke(RiftTheme.sizes.borderThick * 2, playerColor.color)
        } else {
            BorderStroke(RiftTheme.sizes.borderThin, RiftTheme.colorScheme.outlineVariant)
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                RepeatingClickBox(
                    contentDescription = pluralStringResource(
                        R.plurals.feature_tracker_decrease,
                        panel.points,
                        panel.displayName,
                        panel.points,
                    ),
                    onClick = { change(-1) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        imageVector = RiftIcons.Remove,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(RiftTheme.spacing.lg)
                            .size(RiftTheme.sizes.minTouchTarget),
                    )
                }
                RepeatingClickBox(
                    contentDescription = pluralStringResource(
                        R.plurals.feature_tracker_increase,
                        panel.points,
                        panel.displayName,
                        panel.points,
                    ),
                    onClick = { change(1) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(RiftTheme.spacing.lg)
                            .size(RiftTheme.sizes.minTouchTarget),
                    )
                }
            }
            // Not clickable itself, so taps around the portrait fall through to the zone underneath.
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Rows under the portrait: name chip, optional "reached" label, counters, "+ Counter" buttons.
                // Each is at most one touch target tall; the portrait shrinks to whatever height is left.
                val isTeam = panel.players.size > 1
                val counterRows = panel.players.map {
                    it.counters.size +
                        if (it.counters.size < MAX_COUNTERS_PER_PLAYER) 1 else 0
                }
                // Solo: chip + every counter row stacked. Team: players' columns sit side by side, so
                // only the tallest column counts, plus the shared score line (about two rows tall).
                val rows = (if (panel.isAtTarget) 1 else 0) +
                    if (isTeam) 1 + TEAM_SCORE_ROWS + (counterRows.maxOrNull() ?: 0) else 1 + counterRows.sum()
                val spacing = RiftTheme.spacing.sm
                val reserved = (RiftTheme.sizes.minTouchTarget + spacing) * rows + RiftTheme.spacing.lg * 2
                // Keep clear of the −/+ icons at both edges; a team fits its portraits side by side.
                val iconZone = RiftTheme.sizes.minTouchTarget + RiftTheme.spacing.lg * 2
                val widthForAvatars = maxWidth - iconZone * 2 - spacing * (panel.players.size - 1)
                val avatarSize = minOf(maxHeight - reserved, widthForAvatars / panel.players.size).coerceIn(
                    RiftTheme.sizes.avatarSmall,
                    RiftTheme.sizes.avatarLarge,
                )
                val counterContent: @Composable (PanelPlayerUiState) -> Unit = { player ->
                    PlayerCounters(
                        player = player,
                        hapticsEnabled = hapticsEnabled,
                        onCounterChange = onCounterChange,
                        onCounterRemove = onCounterRemove,
                        onAddCounterClick = onAddCounterClick,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    if (isTeam) {
                        TeamHeader(panel = panel, avatarSize = avatarSize)
                    } else {
                        LegendAvatar(
                            legendName = firstPlayer?.legendName,
                            imageUri = firstPlayer?.legendImageUri,
                            playerName = panel.displayName,
                            playerColor = playerColor,
                            size = avatarSize,
                            score = panel.points,
                        )
                        PlayerNameChip(name = panel.displayName, playerColor = playerColor)
                    }
                    if (panel.isAtTarget) {
                        Text(
                            text = stringResource(R.string.feature_tracker_at_target),
                            style = RiftTheme.typography.labelLarge,
                            color = RiftTheme.colorScheme.onSurface,
                        )
                    }
                    // Counters are per player, even when a team shares the points.
                    if (isTeam) {
                        Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg)) {
                            panel.players.forEach { player ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    PlayerNameChip(
                                        name = player.name,
                                        playerColor = PlayerColors.fromId(player.colorId) ?: PlayerColors.Grey,
                                    )
                                    counterContent(player)
                                }
                            }
                        }
                    } else {
                        panel.players.forEach { player -> counterContent(player) }
                    }
                }
            }
            DeltaBadge(
                value = panel.points,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = RiftTheme.spacing.md),
            )
        }
    }
}

/** 2v2: both teammates' Legends side by side, then the shared score. */
@Composable
private fun TeamHeader(panel: SidePanelUiState, avatarSize: Dp, modifier: Modifier = Modifier) {
    val scoreDescription = pluralStringResource(
        R.plurals.feature_tracker_team_points,
        panel.points,
        panel.displayName,
        panel.points,
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm)) {
            panel.players.forEach { player ->
                LegendAvatar(
                    legendName = player.legendName,
                    imageUri = player.legendImageUri,
                    playerName = player.name,
                    playerColor = PlayerColors.fromId(player.colorId) ?: PlayerColors.Grey,
                    size = avatarSize,
                )
            }
        }
        Crossfade(
            targetState = panel.points,
            label = "teamScore",
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = scoreDescription
                liveRegion = LiveRegionMode.Polite
            },
        ) { points ->
            Text(
                text = points.toString(),
                style = RiftTheme.scoreTypography.scoreLarge,
                color = RiftTheme.colorScheme.onSurface,
            )
        }
    }
}

/** One player's counters (keyed, so state follows the counter when one is removed) and "+ Counter". */
@Composable
private fun PlayerCounters(
    player: PanelPlayerUiState,
    hapticsEnabled: Boolean,
    onCounterChange: (PlayerId, CounterId, delta: Int) -> Unit,
    onCounterRemove: (PlayerId, CounterId) -> Unit,
    onAddCounterClick: (PlayerId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        player.counters.forEach { counter ->
            key(player.playerId, counter.id) {
                CounterRow(
                    label = counter.label,
                    value = counter.value,
                    ownerName = player.name,
                    onValueChange = { delta ->
                        if (hapticsEnabled && (delta > 0 || counter.value > 0)) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                        onCounterChange(player.playerId, counter.id, delta)
                    },
                    onRemove = { onCounterRemove(player.playerId, counter.id) },
                )
            }
        }
        if (player.counters.size < MAX_COUNTERS_PER_PLAYER) {
            AddCounterButton(playerName = player.name, onClick = { onAddCounterClick(player.playerId) })
        }
    }
}

private const val TEAM_SCORE_ROWS = 2 // scoreLarge line height ≈ two touch targets

/** "+ Counter", announced as "Add counter for Ana" (the visible text alone is ambiguous with 2+ players). */
@Composable
private fun AddCounterButton(playerName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.feature_tracker_add_counter_for, playerName)
    RiftTextButton(
        text = stringResource(R.string.feature_tracker_add_counter),
        onClick = onClick,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick {
                onClick()
                true
            }
        },
    )
}

internal val previewPanels = persistentListOf(
    SidePanelUiState(
        sideId = SideId("s1"),
        points = 6,
        isAtTarget = false,
        players = persistentListOf(
            PanelPlayerUiState(
                playerId = PlayerId("p1"),
                name = "Ana",
                colorId = "orange",
                legendName = "Jinx",
                legendImageUri = null,
                counters = persistentListOf(CounterUiState(CounterId("xp"), "XP", 3)),
            ),
        ),
    ),
    SidePanelUiState(
        sideId = SideId("s2"),
        points = 8,
        isAtTarget = true,
        players = persistentListOf(PanelPlayerUiState(PlayerId("p2"), "Ben", "sky", null, null)),
    ),
)

@PreviewRiftThemes
@Composable
private fun SidePanelPreview() {
    RiftTheme {
        Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm)) {
            previewPanels.forEach { panel ->
                SidePanel(
                    panel = panel,
                    hapticsEnabled = false,
                    onPointsChange = {},
                    onCounterChange = { _, _, _ -> },
                    onCounterRemove = { _, _ -> },
                    onAddCounterClick = {},
                    modifier = Modifier.size(RiftTheme.sizes.avatarLarge * 2),
                )
            }
        }
    }
}
