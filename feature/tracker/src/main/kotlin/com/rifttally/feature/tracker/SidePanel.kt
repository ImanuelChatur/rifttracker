package com.rifttally.feature.tracker

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
 * (left half −, right half +, each with a visible icon). Holding a zone repeats. [rotationDegrees]
 * turns the whole panel toward its seat (180° for the player across the table).
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
    rotationDegrees: Float = 0f,
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
        modifier = modifier.rotate(rotationDegrees),
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
                    contentDescription = stringResource(R.string.feature_tracker_decrease, panel.displayName),
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
                    contentDescription = stringResource(R.string.feature_tracker_increase, panel.displayName),
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
                val rows = 1 + (if (panel.isAtTarget) 1 else 0) +
                    panel.players.sumOf { it.counters.size + if (it.counters.size < MAX_COUNTERS_PER_PLAYER) 1 else 0 }
                val spacing = RiftTheme.spacing.sm
                val reserved = (RiftTheme.sizes.minTouchTarget + spacing) * rows + RiftTheme.spacing.lg * 2
                val avatarSize = (maxHeight - reserved).coerceIn(
                    RiftTheme.sizes.avatarMedium,
                    RiftTheme.sizes.avatarLarge,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    LegendAvatar(
                        legendName = firstPlayer?.legendName,
                        imageUri = firstPlayer?.legendImageUri,
                        playerName = panel.displayName,
                        playerColor = playerColor,
                        size = avatarSize,
                        score = panel.points,
                    )
                    PlayerNameChip(name = panel.displayName, playerColor = playerColor)
                    if (panel.isAtTarget) {
                        Text(
                            text = stringResource(R.string.feature_tracker_at_target),
                            style = RiftTheme.typography.labelLarge,
                            color = RiftTheme.colorScheme.onSurface,
                        )
                    }
                    // Counters are per player, even when a team shares the points.
                    panel.players.forEach { player ->
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
            }
        }
    }
}

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
