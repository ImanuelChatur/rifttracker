package com.rifttally.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rifttally.core.designsystem.theme.RiftTheme

/** Low-emphasis text button (dialogs, banners) with a 48dp minimum touch height. */
@Composable
public fun RiftTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        shape = rememberPressMorphShape(interactionSource),
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = RiftTheme.sizes.minTouchTarget),
        enabled = enabled,
    ) {
        ButtonLabel(text = text, icon = icon, style = RiftTheme.typography.labelLarge)
    }
}

@PreviewRiftThemes
@Composable
private fun RiftTextButtonPreview() {
    RiftTheme {
        Row(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            RiftTextButton(text = "End game", onClick = {})
            RiftTextButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}
