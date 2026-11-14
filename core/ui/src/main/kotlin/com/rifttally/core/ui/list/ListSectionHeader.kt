package com.rifttally.core.ui.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rifttally.core.designsystem.theme.RiftTheme

/** A section title above a group of tiles. */
@Composable
public fun ListSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RiftTheme.typography.titleSmall,
        color = RiftTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = RiftTheme.spacing.lg,
                end = RiftTheme.spacing.lg,
                top = RiftTheme.spacing.xl,
                bottom = RiftTheme.spacing.sm,
            )
            .semantics { heading() },
    )
}
