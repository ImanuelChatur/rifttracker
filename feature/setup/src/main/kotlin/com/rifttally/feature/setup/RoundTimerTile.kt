package com.rifttally.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * "Round timer" game option: a switch, and while it is on, the length in minutes with −/+ (5–120, steps
 * of 5). Both are remembered for the next game.
 */
@Composable
internal fun RoundTimerTile(
    roundTimer: RoundTimerSetupUiState,
    onToggle: (Boolean) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = RiftTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                modifier = Modifier
                    .toggleable(value = roundTimer.enabled, role = Role.Switch, onValueChange = onToggle)
                    .heightIn(min = RiftTheme.sizes.heroButtonHeight)
                    .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.feature_setup_round_timer),
                    style = RiftTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // The row handles the toggle, so the switch itself is not separately focusable.
                Switch(checked = roundTimer.enabled, onCheckedChange = null)
            }
            if (roundTimer.enabled) {
                val minutesText = pluralStringResource(
                    R.plurals.feature_setup_round_timer_minutes,
                    roundTimer.minutes,
                    roundTimer.minutes,
                )
                Row(
                    modifier = Modifier
                        .heightIn(min = RiftTheme.sizes.heroButtonHeight)
                        .padding(
                            start = RiftTheme.spacing.lg,
                            end = RiftTheme.spacing.lg,
                            bottom = RiftTheme.spacing.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.feature_setup_round_timer_length),
                        style = RiftTheme.typography.bodyLarge,
                        color = RiftTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    StepButton(
                        icon = { Icon(RiftIcons.Remove, contentDescription = null) },
                        description = stringResource(R.string.feature_setup_round_timer_decrease),
                        onClick = onDecrease,
                        enabled = roundTimer.canDecrease,
                    )
                    Text(
                        text = stringResource(R.string.feature_setup_round_timer_value, roundTimer.minutes),
                        style = RiftTheme.scoreTypography.counter,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(min = RiftTheme.sizes.avatarMedium)
                            .clearAndSetSemantics { contentDescription = minutesText },
                    )
                    StepButton(
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        description = stringResource(R.string.feature_setup_round_timer_increase),
                        onClick = onIncrease,
                        enabled = roundTimer.canIncrease,
                    )
                }
            }
        }
    }
}

@PreviewRiftThemes
@Composable
private fun RoundTimerTilePreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
        ) {
            RoundTimerTile(
                roundTimer = RoundTimerSetupUiState(enabled = false),
                onToggle = {},
                onIncrease = {},
                onDecrease = {},
                shape = RiftTheme.shapes.segment(0, 2),
            )
            RoundTimerTile(
                roundTimer = RoundTimerSetupUiState(enabled = true, minutes = 50),
                onToggle = {},
                onIncrease = {},
                onDecrease = {},
                shape = RiftTheme.shapes.segment(1, 2),
            )
        }
    }
}
