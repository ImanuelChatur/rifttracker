package com.rifttally.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.titleMediumEmphasized

/**
 * The one hero action of a screen (DESIGN.md §2.2, §9): a filled pill at least `RiftTheme.sizes.heroButtonHeight`
 * (56dp) tall, with the press morph and an *emphasized* label (§5: emphasized type for the hero only). Use at
 * most one per screen region; other actions use [RiftTonalButton] or [RiftTextButton].
 *
 * @param icon optional decorative leading icon (the [text] names the action).
 */
@Composable
public fun RiftHeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        shape = rememberPressMorphShape(interactionSource),
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = RiftTheme.sizes.heroButtonHeight),
        enabled = enabled,
    ) {
        ButtonLabel(text = text, icon = icon, style = RiftTheme.typography.titleMediumEmphasized)
    }
}

@PreviewRiftThemes
@Composable
private fun RiftHeroButtonPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            RiftHeroButton(text = "Start", onClick = {}, modifier = Modifier.fillMaxWidth())
            RiftHeroButton(text = "Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}
