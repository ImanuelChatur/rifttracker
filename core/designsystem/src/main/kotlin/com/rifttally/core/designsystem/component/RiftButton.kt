package com.rifttally.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.theme.RiftTheme

/** Primary filled button with a 48dp minimum touch height. */
@Composable
public fun RiftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = RiftTheme.sizes.minTouchTarget),
        enabled = enabled,
    ) {
        Text(text = text, style = RiftTheme.typography.labelLarge)
    }
}

@PreviewRiftThemes
@Composable
private fun RiftButtonPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            RiftButton(text = "New game", onClick = {})
            RiftButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
