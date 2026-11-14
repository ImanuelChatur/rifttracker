package com.rifttally.core.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftTonalButton
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * Empty state (DESIGN.md §9): an icon motif, one sentence and one action. Centered, scrolls at large
 * font scales, and respects the Scaffold insets via [modifier].
 */
@Composable
public fun EmptyState(
    icon: ImageVector,
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Centered while it fits; scrolls at large font scales instead of clipping.
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .padding(RiftTheme.spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateContent(icon = icon, text = text, actionLabel = actionLabel, onAction = onAction)
        }
    }
}

/** The body of [EmptyState], for use inside a list (no own scrolling or fill). */
@Composable
public fun EmptyStateContent(
    icon: ImageVector,
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = RiftTheme.shapes.full, color = RiftTheme.colorScheme.secondaryContainer) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RiftTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(RiftTheme.spacing.lg)
                    .size(RiftTheme.sizes.avatarSmall),
            )
        }
        Text(
            text = text,
            style = RiftTheme.typography.bodyLarge,
            color = RiftTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = RiftTheme.sizes.listMaxContentWidth),
        )
        RiftTonalButton(text = actionLabel, onClick = onAction)
    }
}

@PreviewRiftThemes
@Composable
private fun EmptyStatePreview() {
    RiftTheme {
        Surface {
            EmptyState(icon = RiftIcons.History, text = "No games yet.", actionLabel = "New game", onAction = {})
        }
    }
}
