package com.rifttally.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * Max width of list content on wide windows (DESIGN.md §8: "on expanded width, limit content width
 * (≈ 640dp)"). No design-system token exists for it yet; replace with one when it does.
 */
private val ListMaxContentWidth: Dp = 640.dp

/**
 * A full-height list that respects the [Scaffold][androidx.compose.material3.Scaffold] insets
 * ([scaffoldPadding]) as content padding, so rows scroll behind the system bars (edge-to-edge), and
 * keeps its content in a centered column of at most [ListMaxContentWidth] on wide windows.
 */
@Composable
internal fun CenteredList(
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val insetStart = scaffoldPadding.calculateStartPadding(layoutDirection)
        val insetEnd = scaffoldPadding.calculateEndPadding(layoutDirection)
        val gutter = RiftTheme.spacing.lg
        val side = maxOf(gutter, (maxWidth - insetStart - insetEnd - ListMaxContentWidth) / 2)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = insetStart + side,
                end = insetEnd + side,
                top = scaffoldPadding.calculateTopPadding() + RiftTheme.spacing.sm,
                bottom = scaffoldPadding.calculateBottomPadding() + RiftTheme.spacing.xl,
            ),
            // Small gaps between tiles instead of dividers (segmented list, DESIGN.md §8).
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
            content = content,
        )
    }
}

/** A section title above a group of tiles. */
@Composable
internal fun ListSectionHeader(text: String, modifier: Modifier = Modifier) {
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

/**
 * One tile of a segmented list (DESIGN.md §8): its own tonal `surfaceContainer` surface (no border, no
 * shadow), larger outer corners at the ends of its group of [count].
 */
@Composable
internal fun SegmentedTile(index: Int, count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.segment(index, count),
        color = RiftTheme.colorScheme.surfaceContainer,
        tonalElevation = RiftTheme.elevation.level0,
        content = content,
    )
}
