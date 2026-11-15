package com.rifttally.core.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * One tile of a segmented list (DESIGN.md §8): its own tonal `surfaceContainer` surface (no border, no
 * shadow), larger outer corners at the ends of its group of [count]. Clickable when [onClick] is set;
 * [onClickLabel] tells screen readers what the tap does ("Open game").
 */
@Composable
public fun SegmentedTile(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val shape = RiftTheme.shapes.segment(index, count)
    val color = RiftTheme.colorScheme.surfaceContainer
    if (onClick != null) {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            Box(modifier = Modifier.fillMaxWidth().clickable(onClickLabel = onClickLabel, onClick = onClick)) {
                content()
            }
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, content = content)
    }
}

@PreviewRiftThemes
@Composable
private fun SegmentedTilePreview() {
    RiftTheme {
        Column {
            ListSectionHeader(text = "Section")
            repeat(3) { index ->
                SegmentedTile(index = index, count = 3, modifier = Modifier.padding(bottom = RiftTheme.spacing.xxs)) {
                    Text(text = "Tile ${index + 1}", modifier = Modifier.padding(RiftTheme.spacing.lg))
                }
            }
        }
    }
}
