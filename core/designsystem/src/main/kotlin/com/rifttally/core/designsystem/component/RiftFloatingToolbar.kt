package com.rifttally.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * A floating toolbar (DESIGN.md §8), mirroring Material's `HorizontalFloatingToolbar` (1.5.0 only):
 * a full-radius pill on a translucent `surfaceContainer`, level-3 shadow, ≥ 48dp tall. Put
 * [RiftIconButton]s (and short labels) in [content].
 */
@Composable
public fun RiftFloatingToolbar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RiftTheme.shapes.full,
        color = RiftTheme.colorScheme.surfaceContainer.copy(alpha = TOOLBAR_ALPHA),
        contentColor = RiftTheme.colorScheme.onSurface,
        shadowElevation = RiftTheme.elevation.level3,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = RiftTheme.sizes.controlBarHeight)
                .padding(horizontal = RiftTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private const val TOOLBAR_ALPHA = 0.85f

@PreviewRiftThemes
@Composable
private fun RiftFloatingToolbarPreview() {
    RiftTheme {
        RiftFloatingToolbar(modifier = Modifier.padding(RiftTheme.spacing.lg)) {
            RiftIconButton(imageVector = RiftIcons.Undo, contentDescription = "Undo", onClick = {})
            RiftIconButton(imageVector = RiftIcons.MoreVert, contentDescription = "Menu", onClick = {})
        }
    }
}
