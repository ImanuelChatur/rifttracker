package com.rifttally.core.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * A full-height list that uses the Scaffold insets ([scaffoldPadding]) as content padding, so rows
 * scroll behind the system bars (edge-to-edge), with its content in a centered column of at most
 * `RiftTheme.sizes.listMaxContentWidth` on wide windows. Tiles are separated by small gaps, not
 * dividers (segmented list, DESIGN.md §8).
 */
@Composable
public fun CenteredList(
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val maxContentWidth = RiftTheme.sizes.listMaxContentWidth
    val minGutter = RiftTheme.spacing.lg
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val insetStart = scaffoldPadding.calculateStartPadding(layoutDirection)
        val insetEnd = scaffoldPadding.calculateEndPadding(layoutDirection)
        val side = maxOf(minGutter, (maxWidth - insetStart - insetEnd - maxContentWidth) / 2)
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
