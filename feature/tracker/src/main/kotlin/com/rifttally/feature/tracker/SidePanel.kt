package com.rifttally.feature.tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.SideId
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
            // Not clickable, so taps on the portrait fall through to the zone underneath.
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                LegendAvatar(
                    legendName = firstPlayer?.legendName,
                    imageUri = firstPlayer?.legendImageUri,
                    playerName = panel.displayName,
                    playerColor = playerColor,
                    size = RiftTheme.sizes.avatarLarge,
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
            }
        }
    }
}

internal val previewPanels = persistentListOf(
    SidePanelUiState(
        sideId = SideId("s1"),
        points = 6,
        isAtTarget = false,
        players = persistentListOf(PanelPlayerUiState("Ana", "orange", "Jinx", null)),
    ),
    SidePanelUiState(
        sideId = SideId("s2"),
        points = 8,
        isAtTarget = true,
        players = persistentListOf(PanelPlayerUiState("Ben", "sky", null, null)),
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
                    modifier = Modifier.size(RiftTheme.sizes.avatarLarge * 2),
                )
            }
        }
    }
}
