package com.rifttally.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * Secondary (tonal) pill button, 48dp minimum height, with the press morph (DESIGN.md §9: primary =
 * filled, secondary = tonal, tertiary = text).
 */
@Composable
public fun RiftTonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        shape = rememberPressMorphShape(interactionSource),
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = RiftTheme.sizes.minTouchTarget),
        enabled = enabled,
    ) {
        Text(text = text, style = RiftTheme.typography.labelLarge)
    }
}

@PreviewRiftThemes
@Composable
private fun RiftTonalButtonPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            RiftTonalButton(text = "New game", onClick = {})
            RiftTonalButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
