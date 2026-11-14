package com.rifttally.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.theme.RiftTheme

// Preview-only overview of the design tokens (player palette and score type scale).

@PreviewRiftThemes
@Composable
private fun PlayerPalettePreview() {
    RiftTheme {
        Surface(color = RiftTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(RiftTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                RiftTheme.playerColors.forEachIndexed { index, playerColor ->
                    Box(
                        modifier = Modifier
                            .size(RiftTheme.sizes.avatarSmall)
                            .background(playerColor.color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = RiftTheme.scoreTypography.counter,
                            color = playerColor.onColor,
                        )
                    }
                }
            }
        }
    }
}

@PreviewRiftThemes
@Composable
private fun ScoreTypographyPreview() {
    RiftTheme {
        Surface(color = RiftTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(RiftTheme.spacing.lg)) {
                val styles = RiftTheme.scoreTypography
                listOf(styles.scoreHuge, styles.scoreLarge, styles.scoreMedium, styles.scoreSmall, styles.counter)
                    .forEach { style ->
                        Text(text = "18", style = style, color = RiftTheme.colorScheme.onBackground)
                    }
            }
        }
    }
}
