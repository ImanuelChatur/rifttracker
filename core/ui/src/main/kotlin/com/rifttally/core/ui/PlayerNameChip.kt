package com.rifttally.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * A player's name on a pill in their color. Text uses [PlayerColor.onColor], so it passes WCAG AA in
 * both themes (player colors are too bright to use as text on a light surface).
 */
@Composable
public fun PlayerNameChip(name: String, playerColor: PlayerColor, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = playerColor.color, contentColor = playerColor.onColor) {
        Text(
            text = name,
            style = RiftTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = RiftTheme.spacing.md, vertical = RiftTheme.spacing.xs),
        )
    }
}

@PreviewRiftThemes
@Composable
private fun PlayerNameChipPreview() {
    RiftTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm)) {
            PlayerNameChip(name = "Ana", playerColor = PlayerColors.Orange)
            PlayerNameChip(name = "Ben", playerColor = PlayerColors.Sky)
            PlayerNameChip(name = "A very long player name", playerColor = PlayerColors.Green)
        }
    }
}
