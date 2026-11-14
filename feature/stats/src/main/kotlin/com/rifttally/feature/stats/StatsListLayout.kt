package com.rifttally.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rifttally.core.designsystem.component.RiftTonalButton
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * Max width of list content on wide windows (DESIGN.md §8: "on expanded width, limit content width
 * (≈ 640dp)"). No design-system token exists for it yet; replace with one when it does.
 */
internal val ListMaxContentWidth: Dp = 640.dp

/** Horizontal padding that keeps content in a centered column of at most [ListMaxContentWidth]. */
private fun centeredGutter(available: Dp, minGutter: Dp): Dp = maxOf(minGutter, (available - ListMaxContentWidth) / 2)

/**
 * A full-height list that uses the Scaffold insets ([scaffoldPadding]) as content padding, so rows
 * scroll behind the system bars (edge-to-edge), with its content in a centered column of at most
 * [ListMaxContentWidth] on wide windows. Tiles are separated by small gaps, not dividers (DESIGN.md §8).
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
        val side = centeredGutter(maxWidth - insetStart - insetEnd, RiftTheme.spacing.lg)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = insetStart + side,
                end = insetEnd + side,
                top = scaffoldPadding.calculateTopPadding() + RiftTheme.spacing.sm,
                bottom = scaffoldPadding.calculateBottomPadding() + RiftTheme.spacing.xl,
            ),
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
 * shadow), larger outer corners at the ends of its group of [count]. Clickable when [onClick] is set.
 */
@Composable
internal fun SegmentedTile(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RiftTheme.shapes.segment(index, count)
    val color = RiftTheme.colorScheme.surfaceContainer
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color, content = content)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, content = content)
    }
}

/** Tonal placeholder tiles while local data loads (DESIGN.md §9: no spinners). */
internal fun LazyListScope.skeletonTiles(count: Int = SKELETON_TILES) {
    items(count) { index ->
        SegmentedTile(index = index, count = count) {
            Box(modifier = Modifier.heightIn(min = RiftTheme.sizes.avatarLarge))
        }
    }
}

/**
 * Empty state (DESIGN.md §9): an icon motif, one sentence and one action. Centered, scrolls at large
 * font scales, and respects the Scaffold insets via [modifier].
 */
@Composable
internal fun EmptyState(
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
internal fun EmptyStateContent(
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
            modifier = Modifier.widthIn(max = ListMaxContentWidth),
        )
        RiftTonalButton(text = actionLabel, onClick = onAction)
    }
}

private const val SKELETON_TILES = 3
