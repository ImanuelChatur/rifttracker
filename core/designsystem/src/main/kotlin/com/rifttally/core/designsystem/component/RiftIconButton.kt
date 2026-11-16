package com.rifttally.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/** Icon button with a required [contentDescription] and a 48dp minimum touch target. */
@Composable
public fun RiftIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val minTouch = RiftTheme.sizes.minTouchTarget
    IconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = minTouch, minHeight = minTouch),
        enabled = enabled,
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}

@PreviewRiftThemes
@Composable
private fun RiftIconButtonPreview() {
    RiftTheme {
        Row(modifier = Modifier.padding(RiftTheme.spacing.sm)) {
            RiftIconButton(imageVector = RiftIcons.Settings, contentDescription = "Settings", onClick = {})
            RiftIconButton(imageVector = RiftIcons.Add, contentDescription = "Add", onClick = {}, enabled = false)
        }
    }
}
