package com.rifttally.core.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.theme.RiftTheme

/** Tonal placeholder tiles while local data loads (DESIGN.md §9: no spinners). */
public fun LazyListScope.skeletonTiles(count: Int = SKELETON_TILES) {
    items(count) { index ->
        SegmentedTile(index = index, count = count) {
            Box(modifier = Modifier.heightIn(min = RiftTheme.sizes.avatarLarge))
        }
    }
}

private const val SKELETON_TILES = 3
