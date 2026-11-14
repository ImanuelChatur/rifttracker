package com.rifttally.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.theme.RiftTheme

/** Optional leading icon (decorative: the label names the action) and the label of a Rift button. */
@Composable
internal fun ButtonLabel(text: String, icon: ImageVector?, style: TextStyle, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(RiftTheme.sizes.iconSmall))
        }
        Text(text = text, style = style, textAlign = TextAlign.Center)
    }
}
