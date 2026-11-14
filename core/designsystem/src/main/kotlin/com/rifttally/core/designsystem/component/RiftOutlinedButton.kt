package com.rifttally.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.theme.RiftTheme

/** Secondary (outlined) button with a 48dp minimum touch height. */
@Composable
public fun RiftOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedButton(
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
private fun RiftOutlinedButtonPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            RiftOutlinedButton(text = "Keep playing", onClick = {})
            RiftOutlinedButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
